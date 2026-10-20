package io.github.mdw22.paranormalcraft.familiar;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import io.github.mdw22.paranormalcraft.ward.Cooldowns;
import io.github.mdw22.paranormalcraft.ward.WardEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The Wisp Familiar: a bound Lingering Spirit as a small glowing orb that follows its owner. Visible to everyone (no
 * goggles), invulnerable, no collision, never haunts, never saved: it exists only while summoned from its Bound
 * Spirit Vessel, and {@link Familiars} recalls it when that link breaks.
 * <ul>
 *     <li><b>Follow:</b> 2–3 blocks behind the owner's shoulder, with a soft bob; teleports past
 *     {@link #TELEPORT_DISTANCE}.</li>
 *     <li><b>Light:</b> carries a vanilla light block where it is dark ({@link WispLight}, tracked by
 *     {@link WispLights}).</li>
 *     <li><b>Warnings:</b> a hostile the owner can't see ({@link WispWarnings}) turns it red and draws it to that
 *     side, with a chirp from its own position once per mob per {@link #WARN_COOLDOWN_TICKS}.</li>
 *     <li><b>Guide:</b> flies {@link #GUIDE_AHEAD} blocks toward a point and waits there, or shakes "no".</li>
 * </ul>
 */
public class WispFamiliarEntity extends Entity {

    public static final double TELEPORT_DISTANCE = Balance.Familiar.TELEPORT_DISTANCE;
    public static final int WARN_COOLDOWN_TICKS = Balance.Familiar.WARN_COOLDOWN_TICKS;
    public static final double GUIDE_AHEAD = Balance.Familiar.GUIDE_AHEAD;
    private static final double FOLLOW_OFFSET = (Balance.Familiar.FOLLOW_MIN + Balance.Familiar.FOLLOW_MAX) / 2.0 / Math.sqrt(2.0);
    private static final DustParticleOptions WARN_MOTE = new DustParticleOptions(0xFF4030, 0.8F);

    private static final EntityDataAccessor<Boolean> DATA_WARNING = SynchedEntityData.defineId(WispFamiliarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GUIDING = SynchedEntityData.defineId(WispFamiliarEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID owner;
    private @Nullable UUID ghost;
    private String ghostName = "";
    private @Nullable BlockPos light;
    private @Nullable Vec3 threatSide;
    private final Cooldowns<UUID> warned = new Cooldowns<>();
    private @Nullable Vec3 guideTarget;
    private int guideTicks;
    private int shakeTicks;

    public WispFamiliarEntity(EntityType<? extends WispFamiliarEntity> type, Level level) {

        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** Server: ties a new wisp to its owner and the bound ghost it comes from. */
    public void bindTo(ServerPlayer owner, UUID ghost, String ghostName) {

        this.owner = owner.getUUID();
        this.ghost = ghost;
        this.ghostName = ghostName;
    }

    public @Nullable UUID owner() {

        return this.owner;
    }

    public @Nullable UUID ghost() {

        return this.ghost;
    }

    public String ghostName() {

        return this.ghostName;
    }

    public @Nullable BlockPos light() {

        return this.light;
    }

    public boolean isWarning() {

        return this.entityData.get(DATA_WARNING);
    }

    public boolean isGuiding() {

        return this.entityData.get(DATA_GUIDING);
    }

    public boolean isShaking() {

        return this.shakeTicks > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

        builder.define(DATA_WARNING, false);
        builder.define(DATA_GUIDING, false);
    }

    // ---- ticking ----

    @Override
    public void tick() {

        super.tick();
        if (this.level().isClientSide()) {
            clientTick();
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer player = this.owner == null ? null : level.getServer().getPlayerList().getPlayer(this.owner);
        if (player == null || player.level() != level) {
            // Familiars recalls it on its own tick, with the reason; until then it just waits.
            return;
        }
        if (this.tickCount % Balance.Familiar.WARN_SCAN_TICKS == 0) {
            scanThreats(level, player);
        }
        move(player);
        updateLight(level);
    }

    private void clientTick() {

        Level level = this.level();
        if (this.tickCount % 3 == 0) {
            level.addParticle(isWarning() ? WARN_MOTE : WardEffects.MOTE, getX() + (this.random.nextDouble() - 0.5) * 0.2,
                    getY() + 0.1, getZ() + (this.random.nextDouble() - 0.5) * 0.2, 0.0, 0.0, 0.0);
        }
        if (isGuiding() && this.tickCount % 4 == 0) {
            level.addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 0.0, 0.02, 0.0);
        }
    }

    private void move(ServerPlayer player) {

        Vec3 base = player.position().add(0.0, Balance.Familiar.SHOULDER_HEIGHT, 0.0);
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        Vec3 target;
        if (this.guideTarget != null) {
            target = this.guideTarget;
            if (--this.guideTicks <= 0 || player.position().distanceTo(this.guideTarget) > TELEPORT_DISTANCE + GUIDE_AHEAD) {
                endGuide("done");
            }
        } else if (this.threatSide != null) {
            target = base.add(this.threatSide.scale(Balance.Familiar.WARN_DRIFT));
        } else {
            target = base.add(right.scale(FOLLOW_OFFSET)).add(look.scale(-FOLLOW_OFFSET));
        }
        target = target.add(0.0, Math.sin(this.tickCount * 0.1) * 0.12, 0.0);
        if (this.shakeTicks > 0) {
            this.shakeTicks--;
            target = target.add(right.scale(Math.sin(this.shakeTicks * 1.3) * 0.45));
        }
        target = inTheOpen(player, target);
        if (this.guideTarget == null && position().distanceTo(base) > TELEPORT_DISTANCE) {
            setPos(target);
            return;
        }
        double rate = this.shakeTicks > 0 ? 0.5 : Balance.Familiar.FOLLOW_RATE;
        Vec3 next = position().lerp(target, rate);
        setDeltaMovement(next.subtract(position()));
        setPos(next);
    }

    /**
     * The wisp passes through walls, but it shouldn't sit inside one (it would hide there, and can't light it): a
     * target behind a wall, seen from the owner's eyes, is pulled back to just short of the wall.
     */
    private Vec3 inTheOpen(ServerPlayer player, Vec3 target) {

        Vec3 eyes = player.getEyePosition();
        BlockHitResult hit = this.level().clip(new ClipContext(eyes, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return target;
        }
        Vec3 back = target.subtract(eyes);
        double length = back.length();
        if (length < 1.0E-6) {
            return target;
        }
        return hit.getLocation().subtract(back.scale(Math.min(0.35, hit.getLocation().distanceTo(eyes)) / length));
    }

    // ---- warnings ----

    private void scanThreats(ServerLevel level, ServerPlayer player) {

        List<Mob> unseen = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(WispWarnings.WARN_RANGE),
                        mob -> mob instanceof Enemy && !(mob instanceof GhostEntity) && mob.isAlive()
                                && mob.distanceTo(player) <= WispWarnings.WARN_RANGE
                                && WispWarnings.unseen(player.getViewVector(1.0F), mob.getEyePosition().subtract(player.getEyePosition()),
                                player.hasLineOfSight(mob)))
                .stream().sorted(Comparator.comparingDouble(mob -> mob.distanceToSqr(player))).toList();
        boolean warning = !unseen.isEmpty();
        if (warning != isWarning()) {
            this.entityData.set(DATA_WARNING, warning);
        }
        if (!warning) {
            this.threatSide = null;
            return;
        }
        Mob nearest = unseen.getFirst();
        this.threatSide = WispWarnings.side(nearest.position().subtract(player.position()));
        for (Mob mob : unseen) {
            if (this.warned.tryUse(mob.getUUID(), level.getGameTime(), WARN_COOLDOWN_TICKS)) {
                boolean creeper = mob instanceof Creeper;
                level.playSound(null, getX(), getY(), getZ(), creeper ? ModSounds.WISP_WARN_SHARP.get() : ModSounds.WISP_WARN.get(),
                        SoundSource.NEUTRAL, 1.0F, 1.0F);
                double angle = WispWarnings.angle(player.getViewVector(1.0F), mob.getEyePosition().subtract(player.getEyePosition()));
                boolean los = player.hasLineOfSight(mob);
                ServerTrace.event("wisp_warn", line -> {
                    line.addProperty("owner", player.getGameProfile().name());
                    line.addProperty("mob", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
                    line.addProperty("angle", ServerTrace.round(angle));
                    line.addProperty("los", los);
                    line.addProperty("creeper", creeper);
                    line.addProperty("distance", ServerTrace.round(mob.distanceTo(player)));
                });
                // One chirp a scan, for the nearest new threat.
                break;
            }
        }
    }

    // ---- guide ----

    /** Flies toward {@code point} and waits there. */
    public void guideToward(ServerPlayer player, Vec3 point) {

        Vec3 flat = new Vec3(point.x - player.getX(), 0.0, point.z - player.getZ());
        Vec3 direction = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
        this.guideTarget = player.position().add(direction.scale(GUIDE_AHEAD)).add(0.0, Balance.Familiar.SHOULDER_HEIGHT, 0.0);
        this.guideTicks = Balance.Familiar.GUIDE_WAIT_TICKS;
        this.entityData.set(DATA_GUIDING, true);
    }

    /** Shakes "no" and stays. */
    public void shakeNo() {

        this.shakeTicks = Balance.Familiar.SHAKE_TICKS;
    }

    public @Nullable Vec3 guideTarget() {

        return this.guideTarget;
    }

    private void endGuide(String reason) {

        this.guideTarget = null;
        this.guideTicks = 0;
        this.entityData.set(DATA_GUIDING, false);
        ServerTrace.event("wisp_guide_end", line -> line.addProperty("reason", reason));
    }

    // ---- light ----

    private void updateLight(ServerLevel level) {

        BlockPos here = BlockPos.containing(getX(), getY() + 0.2, getZ());
        if (here.equals(this.light)) {
            return;
        }
        int ownDistance = this.light == null ? -1 : this.light.distManhattan(here);
        int blockLight = level.getBrightness(LightLayer.BLOCK, here);
        int skyLight = level.getBrightness(LightLayer.SKY, here) - level.getSkyDarken();
        boolean dark = WispLight.underlyingDark(blockLight, skyLight, ownDistance);
        if (this.light != null) {
            removeLight(level, "moved");
        }
        if (dark && WispLights.place(level, here)) {
            this.light = here;
            ServerTrace.event("wisp_light_place", line -> {
                line.addProperty("pos", here.toShortString());
                line.addProperty("blockLight", blockLight);
                line.addProperty("skyLight", skyLight);
            });
        }
    }

    /** Removes the light it carries, if any. */
    public void removeLight(ServerLevel level, String reason) {

        BlockPos pos = this.light;
        if (pos == null) {
            return;
        }
        this.light = null;
        boolean removed = WispLights.remove(level, pos);
        ServerTrace.event("wisp_light_remove", line -> {
            line.addProperty("pos", pos.toShortString());
            line.addProperty("reason", reason);
            line.addProperty("removed", removed);
        });
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {

        super.onRemoval(reason);
        if (this.level() instanceof ServerLevel level) {
            removeLight(level, reason == RemovalReason.UNLOADED_TO_CHUNK ? "unload" : "removed");
            Familiars.forget(this, reason == RemovalReason.UNLOADED_TO_CHUNK ? "unload" : null);
        }
    }

    // ---- no interaction, no saving ----

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        return false;
    }

    @Override
    public boolean isPickable() {

        return false;
    }

    @Override
    public boolean isPushable() {

        return false;
    }

    @Override
    public boolean isAttackable() {

        return false;
    }

    @Override
    public boolean shouldBeSaved() {

        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
