package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.config.ServerConfig;
import io.github.mdw22.paranormalcraft.tool.GhostEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for ghosts: an invisible, flying, home-bound mob whose gameplay meter is {@link Presence}, not health.
 * <p>
 * Visibility comes from {@link GhostVisibility}. On the client, {@link #isInvisible()} and {@link #isPickable()}
 * answer for the local player, which hides the model, shadow, name tag and hitbox and stops crosshair picking without
 * a mixin. On the server, hits only count while the attacker can see the ghost.
 */
public abstract class GhostEntity extends PathfinderMob {

    /** Idle wandering stays within this many blocks of home. */
    public static final int WANDER_RADIUS = 12;
    /** Beyond this the ghost drifts back home. */
    public static final int LEASH_RADIUS = 16;
    /** How far players can be for curiosity and manifesting. */
    public static final double PLAYER_RANGE = 10.0;
    /** 1.5 seconds. */
    public static final int MANIFEST_TICKS = 30;
    /** Vanilla health never matters; it is reset after every hit so the ghost only ever disperses. */
    public static final float FIXED_HEALTH = 1024.0F;
    private static final int MANIFEST_RETRY_TICKS = 20;
    /** Vanilla's spectral arrow Glowing duration (its private default). */
    private static final int SPECTRAL_ARROW_GLOWING_TICKS = 200;

    private static final EntityDataAccessor<Float> DATA_PRESENCE = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_MANIFESTING = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.BOOLEAN);

    private Presence presence = new Presence();
    private @Nullable BlockPos home;
    /** Where the thermometer reads coldest; chosen when home is first set. */
    private @Nullable BlockPos coldSpot;
    /** Ticks until the next manifest attempt; -1 until first scheduled. Server only. */
    private int manifestCooldown = -1;
    private int manifestTicksLeft;
    /** Client only: tick count when the current manifest started, for the fade-in. */
    private int manifestStartTick = Integer.MIN_VALUE;

    protected GhostEntity(EntityType<? extends GhostEntity> type, Level level) {

        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createGhostAttributes() {

        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, FIXED_HEALTH)
                .add(Attributes.FLYING_SPEED, 0.05F)
                .add(Attributes.MOVEMENT_SPEED, 0.1F)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    protected abstract SoundEvent getWhisperSound();

    protected abstract SoundEvent getDisperseSound();

    // ---- State ----

    public boolean isManifesting() {

        return this.entityData.get(DATA_MANIFESTING);
    }

    public float getPresence() {

        return this.entityData.get(DATA_PRESENCE);
    }

    public @Nullable BlockPos getHome() {

        return this.home;
    }

    public @Nullable BlockPos getColdSpot() {

        return this.coldSpot;
    }

    /**
     * Simple names of the AI goals running right now, for debugging.
     */
    public String activeGoals() {

        String running = this.goalSelector.getAvailableGoals().stream()
                .filter(WrappedGoal::isRunning)
                .map(goal -> goal.getGoal().getClass().getSimpleName())
                .collect(Collectors.joining(", "));
        return running.isEmpty() ? "none" : running;
    }

    /**
     * Debug: sets Presence (clamped to 0..max). Zero disperses the ghost.
     */
    public void setPresence(ServerLevel level, float value) {

        this.presence = new Presence(value, this.presence.ticksSinceHit());
        this.entityData.set(DATA_PRESENCE, this.presence.value());
        if (this.presence.isDepleted()) {
            disperse(level);
        }
    }

    /**
     * The home position, falling back to the current position before home is set.
     */
    public BlockPos homeOrHere() {

        return this.home != null ? this.home : this.blockPosition();
    }

    public double distanceToHomeSqr() {

        return this.position().distanceToSqr(Vec3.atCenterOf(homeOrHere()));
    }

    /**
     * Client side: ticks since the current manifest began, or -1 if not manifesting.
     */
    public float manifestAge(float partialTick) {

        return isManifesting() && this.manifestStartTick != Integer.MIN_VALUE ? this.tickCount - this.manifestStartTick + partialTick : -1.0F;
    }

    /**
     * Whether {@code player} is looking straight at this ghost, using the enderman's look-angle check.
     */
    public boolean isLookedAtBy(Player player) {

        return this.isLookingAtMe(player, 0.025, true, false, this.getEyeY(), this.getY() + this.getBbHeight() * 0.5);
    }

    /**
     * Living, non-spectator players within {@link #PLAYER_RANGE}.
     */
    public List<Player> playersInRange() {

        return this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(PLAYER_RANGE),
                player -> player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= PLAYER_RANGE * PLAYER_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {

        super.defineSynchedData(entityData);
        entityData.define(DATA_PRESENCE, Presence.MAX);
        entityData.define(DATA_MANIFESTING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {

        super.onSyncedDataUpdated(accessor);
        if (DATA_MANIFESTING.equals(accessor) && this.level().isClientSide() && isManifesting()) {
            this.manifestStartTick = this.tickCount;
        }
    }

    // ---- Ticking ----

    @Override
    public void tick() {

        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.home == null) {
                this.home = this.blockPosition();
                this.coldSpot = ColdSpots.choose(serverLevel, this.home, WANDER_RADIUS, this.random);
            }
            this.presence.tick();
            this.entityData.set(DATA_PRESENCE, this.presence.value());
            tickManifest(serverLevel);
        }
    }

    private void tickManifest(ServerLevel level) {

        if (this.manifestTicksLeft > 0) {
            if (--this.manifestTicksLeft == 0) {
                this.entityData.set(DATA_MANIFESTING, false);
            }
            return;
        }
        if (this.manifestCooldown < 0) {
            this.manifestCooldown = rollManifestCooldown();
        }
        if (this.manifestCooldown > 0) {
            this.manifestCooldown--;
            return;
        }
        boolean someoneUnaware = playersInRange().stream().anyMatch(player -> !isLookedAtBy(player));
        if (someoneUnaware) {
            manifest(level);
            this.manifestCooldown = rollManifestCooldown();
        } else {
            this.manifestCooldown = MANIFEST_RETRY_TICKS;
        }
    }

    /**
     * Becomes visible to everyone for {@link #MANIFEST_TICKS} and whispers.
     */
    public void manifest(ServerLevel level) {

        this.manifestTicksLeft = MANIFEST_TICKS;
        this.entityData.set(DATA_MANIFESTING, true);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), getWhisperSound(), this.getSoundSource(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
        GhostEvents.onGhostActivity(this);
    }

    private int rollManifestCooldown() {

        ServerConfig config = ModConfigs.server();
        int min = config.manifestIntervalMinSeconds() * 20;
        int max = config.manifestIntervalMaxSeconds() * 20;
        return min + this.random.nextInt(max - min + 1);
    }

    // ---- Damage and dispersal ----

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // /kill and the void: remove the ghost the only way it ever leaves.
            disperse(level);
            return true;
        }
        if (!hitCounts(source)) {
            // A spectral arrow still tags an unseen ghost, as a clue for players without goggles. No Presence damage.
            if (source.getDirectEntity() instanceof SpectralArrow) {
                this.addEffect(new MobEffectInstance(MobEffects.GLOWING, SPECTRAL_ARROW_GLOWING_TICKS, 0), source.getEntity());
            }
            return false;
        }
        float healthBefore = this.getHealth();
        boolean hurt = super.hurtServer(level, source, damage);
        float dealt = healthBefore - this.getHealth();
        this.setHealth(this.getMaxHealth());
        if (dealt > 0.0F) {
            boolean depleted = this.presence.hit(dealt);
            this.entityData.set(DATA_PRESENCE, this.presence.value());
            if (depleted) {
                disperse(level);
            }
        }
        return hurt;
    }

    /**
     * A hit counts only if it comes from an attacker who can see the ghost right now. Environmental damage never
     * counts.
     */
    protected boolean hitCounts(DamageSource source) {

        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FREEZING)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            return GhostVisibility.canSee(player, this);
        }
        // Mobs have no goggles, so they can only hit a manifesting ghost.
        return attacker != null && isManifesting();
    }

    /**
     * Soul burst, dispersal sound, gone. No drop yet.
     */
    public void disperse(ServerLevel level) {

        if (this.isRemoved()) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 40, 0.3, 0.4, 0.3, 0.04);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 12, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), getDisperseSound(), this.getSoundSource(), 1.0F, 1.0F);
        this.discard();
    }

    @Override
    public SoundSource getSoundSource() {

        return SoundSource.HOSTILE;
    }

    // ---- Visibility (client answers for the local player) ----

    @Override
    public boolean isInvisible() {

        if (this.level().isClientSide()) {
            return !GhostVisibility.canClientSee(this);
        }
        return super.isInvisible();
    }

    @Override
    public boolean isPickable() {

        if (this.level().isClientSide() && !GhostVisibility.canClientSee(this)) {
            return false;
        }
        return super.isPickable();
    }

    // ---- Flight, collisions and persistence ----

    @Override
    protected PathNavigation createNavigation(Level level) {

        return GhostMovement.createNavigation(this, level);
    }

    @Override
    public void travel(Vec3 input) {

        this.travelFlying(input, this.getSpeed());
    }

    @Override
    public void move(MoverType type, Vec3 delta) {

        // Drift through doors and thin walls without turning on noPhysics.
        if (type == MoverType.SELF && delta.lengthSqr() > 1.0E-7
                && GhostMovement.onlyPassableBlocksIn(this.level(), this.getBoundingBox().expandTowards(delta))) {
            this.setPos(this.getX() + delta.x, this.getY() + delta.y, this.getZ() + delta.z);
            return;
        }
        super.move(type, delta);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean isPushable() {

        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPushedByFluid() {

        return false;
    }

    @Override
    public boolean canBeLeashed() {

        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {

        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {

        return true;
    }

    @Override
    public boolean canAttack(LivingEntity target) {

        return false;
    }

    // ---- Saving ----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.store("Home", BlockPos.CODEC, this.home);
        }
        if (this.coldSpot != null) {
            output.store("ColdSpot", BlockPos.CODEC, this.coldSpot);
        }
        output.putFloat("Presence", this.presence.value());
        output.putInt("PresenceTicksSinceHit", this.presence.ticksSinceHit());
        output.putInt("ManifestCooldown", this.manifestCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

        super.readAdditionalSaveData(input);
        this.home = input.read("Home", BlockPos.CODEC).orElse(null);
        this.coldSpot = input.read("ColdSpot", BlockPos.CODEC).orElse(this.home);
        this.presence = new Presence(input.getFloatOr("Presence", Presence.MAX), input.getIntOr("PresenceTicksSinceHit", Presence.REGEN_DELAY_TICKS));
        this.entityData.set(DATA_PRESENCE, this.presence.value());
        this.manifestCooldown = input.getIntOr("ManifestCooldown", -1);
    }
}
