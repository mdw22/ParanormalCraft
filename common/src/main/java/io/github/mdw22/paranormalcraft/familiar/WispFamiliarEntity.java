package io.github.mdw22.paranormalcraft.familiar;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.haunting.HauntingScheduler;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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
 *     <li><b>Guide:</b> leads the way to a cabin along waypoints planned from the heightmap ({@link GuidePlanner}),
 *     each {@link #GUIDE_AHEAD} blocks on and above the terrain or canopy; it waits at each until the owner comes within
 *     4 blocks, circles the cabin once the owner is within 16, and gives up after 90 s, if the owner wanders off the
 *     route, or if it can't make progress even after rising 6 blocks (then it shakes "no").</li>
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
    private @Nullable Guide guide;
    /** Tests only: freezes its movement, to exercise the stuck recovery. */
    private boolean movementBlocked;
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
        if (this.guide != null) {
            guideMove(player);
            return;
        }
        Vec3 target;
        if (this.threatSide != null) {
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
        if (position().distanceTo(base) > TELEPORT_DISTANCE) {
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
                if (creeper) {
                    // More urgent by rhythm: the same chirp, higher, twice in quick succession.
                    HauntingScheduler.schedule(level, Balance.Familiar.URGENT_CHIRP_GAP_TICKS, () -> level.playSound(null, getX(), getY(), getZ(),
                            ModSounds.WISP_WARN_SHARP.get(), SoundSource.NEUTRAL, 1.0F, 1.1F));
                }
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

    /** A guide in progress. */
    private static final class Guide {

        private final Vec3 cabin;
        private final List<Vec3> route = new ArrayList<>();
        private Vec3 previous;
        private Vec3 waypoint;
        private boolean waiting;
        private int ticks;
        private int circleTicks = -1;
        private double bestDistance = Double.MAX_VALUE;
        private int sinceProgress;
        private boolean rose;
        private int losRise;

        Guide(Vec3 cabin, Vec3 start, Vec3 first) {

            this.cabin = cabin;
            this.previous = start;
            this.waypoint = first;
            this.route.add(first);
        }
    }

    private GuidePlanner.Heights heights() {

        return (x, z) -> this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** Leads the way toward {@code cabin}: plans the first waypoint and sets off. */
    public void guideToward(ServerPlayer player, Vec3 cabin) {

        Vec3 start = player.position();
        Vec3 first = GuidePlanner.next(start, cabin, heights());
        this.guide = new Guide(cabin, start, first);
        this.entityData.set(DATA_GUIDING, true);
        traceWaypoint(0, first);
    }

    private void guideMove(ServerPlayer player) {

        Guide guide = this.guide;
        if (++guide.ticks > Balance.Familiar.GUIDE_TIMEOUT_TICKS) {
            endGuide("timeout");
            return;
        }
        if (guide.circleTicks >= 0) {
            circle(guide);
            return;
        }
        if (GuidePlanner.flatDistance(player.position(), guide.cabin) <= Balance.Familiar.GUIDE_ARRIVED) {
            guide.circleTicks = 0;
            ServerTrace.event("wisp_guide_arrived", line -> line.addProperty("waypoints", guide.route.size()));
            return;
        }
        if (GuidePlanner.offRoute(player.position(), guide.previous, guide.waypoint) > Balance.Familiar.GUIDE_OFF_ROUTE) {
            endGuide("wandered");
            return;
        }
        if (guide.waiting) {
            // Keep in the owner's sight where it can: if something hides it, rise a block at a time (up to 4).
            if (this.tickCount % 10 == 0 && guide.losRise < Balance.Familiar.GUIDE_SIGHT_RISE && !player.hasLineOfSight(this)) {
                guide.losRise++;
                guide.waypoint = guide.waypoint.add(0.0, 1.0, 0.0);
            }
            hover(guide.waypoint.add(0.0, Math.sin(this.tickCount * 0.15) * 0.15, 0.0));
            if (GuidePlanner.flatDistance(player.position(), guide.waypoint) <= Balance.Familiar.GUIDE_REACHED) {
                guide.previous = guide.waypoint;
                guide.waypoint = GuidePlanner.next(guide.waypoint, guide.cabin, heights());
                guide.route.add(guide.waypoint);
                guide.waiting = false;
                guide.losRise = 0;
                guide.bestDistance = Double.MAX_VALUE;
                guide.sinceProgress = 0;
                traceWaypoint(guide.route.size() - 1, guide.waypoint);
            }
            return;
        }
        Vec3 heading = GuidePlanner.heading(position(), guide.waypoint);
        fly(heading);
        double distance = position().distanceTo(guide.waypoint);
        if (distance < Balance.Familiar.GUIDE_ARRIVE_DISTANCE) {
            guide.waiting = true;
            return;
        }
        if (distance < guide.bestDistance - 0.1) {
            guide.bestDistance = distance;
            guide.sinceProgress = 0;
        } else if (++guide.sinceProgress >= Balance.Familiar.GUIDE_STUCK_TICKS) {
            guide.sinceProgress = 0;
            guide.bestDistance = Double.MAX_VALUE;
            if (guide.rose) {
                ServerTrace.event("wisp_guide_stuck", line -> line.addProperty("action", "give_up"));
                shakeNo();
                endGuide("stuck");
                return;
            }
            guide.rose = true;
            guide.waypoint = guide.waypoint.add(0.0, Balance.Familiar.GUIDE_STUCK_RISE, 0.0);
            ServerTrace.event("wisp_guide_stuck", line -> line.addProperty("action", "rise"));
        }
    }

    /** Once round the cabin, high above its roof, then back to the owner. */
    private void circle(Guide guide) {

        double angle = 2 * Math.PI * guide.circleTicks / Balance.Familiar.GUIDE_CIRCLE_TICKS;
        int top = heights().top((int) Math.floor(guide.cabin.x), (int) Math.floor(guide.cabin.z));
        Vec3 point = new Vec3(guide.cabin.x + Math.cos(angle) * Balance.Familiar.GUIDE_CIRCLE_RADIUS, top + Balance.Familiar.GUIDE_CLEARANCE + Balance.Familiar.GUIDE_CIRCLE_LIFT,
                guide.cabin.z + Math.sin(angle) * Balance.Familiar.GUIDE_CIRCLE_RADIUS);
        fly(GuidePlanner.heading(position(), point));
        if (++guide.circleTicks >= Balance.Familiar.GUIDE_CIRCLE_TICKS) {
            endGuide("arrived");
        }
    }

    private void fly(Vec3 target) {

        if (this.movementBlocked) {
            return;
        }
        Vec3 delta = target.subtract(position());
        double length = delta.length();
        Vec3 step = length > Balance.Familiar.GUIDE_SPEED ? delta.scale(Balance.Familiar.GUIDE_SPEED / length) : delta;
        setDeltaMovement(step);
        setPos(position().add(step));
    }

    private void hover(Vec3 target) {

        if (!this.movementBlocked) {
            setPos(position().lerp(target, 0.2));
        }
    }

    private void traceWaypoint(int index, Vec3 waypoint) {

        ServerTrace.event("wisp_guide_waypoint", line -> {
            line.addProperty("index", index);
            line.addProperty("x", ServerTrace.round(waypoint.x));
            line.addProperty("y", ServerTrace.round(waypoint.y));
            line.addProperty("z", ServerTrace.round(waypoint.z));
        });
    }

    /** Shakes "no" and stays. */
    public void shakeNo() {

        this.shakeTicks = Balance.Familiar.SHAKE_TICKS;
    }

    /** The waypoint it is heading for or waiting at, or null. */
    public @Nullable Vec3 guideTarget() {

        return this.guide == null ? null : this.guide.waypoint;
    }

    /** The guide's waypoints so far (empty with no guide). */
    public List<Vec3> guideRoute() {

        return this.guide == null ? List.of() : List.copyOf(this.guide.route);
    }

    /** Whether it is waiting at a waypoint for the owner. */
    public boolean guideWaiting() {

        return this.guide != null && this.guide.waiting;
    }

    /** Tests only: freezes (or frees) its movement. */
    public void setMovementBlocked(boolean blocked) {

        this.movementBlocked = blocked;
    }

    private void endGuide(String reason) {

        int waypoints = this.guide == null ? 0 : this.guide.route.size();
        this.guide = null;
        this.entityData.set(DATA_GUIDING, false);
        ServerTrace.event("wisp_guide_end", line -> {
            line.addProperty("reason", reason);
            line.addProperty("waypoints", waypoints);
        });
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
