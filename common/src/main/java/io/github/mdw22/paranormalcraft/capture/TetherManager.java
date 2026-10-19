package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostMovement;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.item.TetherWandItem;
import io.github.mdw22.paranormalcraft.network.BeamStatePayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server side of the Tether Wand: one beam per player, decided entirely on the server.
 * <ul>
 *     <li>While a player holds use with a wand, {@link #fire} runs each tick: the wand heats (and wears) and the beam
 *     reaches {@link #RANGE} blocks toward the crosshair, latching onto the first ghost the player can see within
 *     {@link #LATCH_DISTANCE} of the beam line.</li>
 *     <li>A latched tether drains 2 Presence a second, the ghost pulls away (with jerks, unless two or more tethers
 *     hold it), and a player more than {@link #PULL_DISTANCE} away is drawn toward it.</li>
 *     <li>It breaks on release, beyond {@link #BREAK_RANGE}, after {@link #LOS_GRACE_TICKS} without line of sight
 *     ({@code ghost_passable} blocks don't block it), when the player can no longer see the ghost, or on overheat.</li>
 *     <li>Two beams that cross (outside their last 1.5 blocks) backfire: both wands overheat and both players are
 *     knocked back and take 2 hearts of magic damage. No blocks are touched.</li>
 * </ul>
 * Every tick the active beams go to the players in that level ({@link BeamStatePayload}) for rendering.
 */
public final class TetherManager {

    public static final double RANGE = Balance.Tether.RANGE;
    public static final double LATCH_DISTANCE = Balance.Tether.LATCH_DISTANCE;
    public static final double BREAK_RANGE = Balance.Tether.BREAK_RANGE;
    public static final int LOS_GRACE_TICKS = Balance.Tether.LOS_GRACE_TICKS;
    public static final double PULL_DISTANCE = Balance.Tether.PULL_DISTANCE;
    public static final double PULL_PER_TICK = Balance.Tether.PULL_PER_TICK;
    public static final float BACKFIRE_DAMAGE = Balance.Tether.BACKFIRE_DAMAGE;
    private static final double BACKFIRE_KNOCKBACK = Balance.Tether.BACKFIRE_KNOCKBACK;

    /** One player's beam. */
    public static final class Tether {

        private final UUID player;
        private @Nullable UUID ghost;
        private int losLostTicks;
        private long lastFireTick;
        private Vec3 start = Vec3.ZERO;
        private Vec3 end = Vec3.ZERO;

        private Tether(UUID player) {

            this.player = player;
        }

        public @Nullable UUID ghost() {

            return this.ghost;
        }

        public Vec3 start() {

            return this.start;
        }

        public Vec3 end() {

            return this.end;
        }
    }

    private static final Map<UUID, Tether> TETHERS = new LinkedHashMap<>();
    /** Players {@code debug tether} keeps firing without holding use (scripts and tests). */
    private static final Set<UUID> FORCED = new HashSet<>();
    /** Ghosts given tethers last tick, so they can be cleared when released. */
    private static final Set<GhostEntity> TETHERED = new HashSet<>();
    private static boolean beamsSentLastTick;

    private TetherManager() {
    }

    // ---- firing ----

    /**
     * One tick of a player firing {@code wand}: from the wand's use tick, or from {@link #tick} for forced players.
     */
    public static void fire(ServerPlayer player, ItemStack wand) {

        ServerLevel level = player.level();
        long now = level.getGameTime();
        Tether tether = TETHERS.computeIfAbsent(player.getUUID(), Tether::new);
        if (tether.lastFireTick == now) {
            return;
        }
        tether.lastFireTick = now;
        WandHeat heat = wand.getOrDefault(ModDataComponents.WAND_HEAT.get(), WandHeat.COLD);
        if (!heat.canFire()) {
            end(player, "overheat");
            return;
        }
        heat = heat.fire();
        if (heat.durabilityDue()) {
            heat = heat.charged();
            EquipmentSlot slot = player.getMainHandItem() == wand ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            wand.hurtAndBreak(1, player, slot);
        }
        wand.set(ModDataComponents.WAND_HEAT.get(), heat);
        if (heat.overheated()) {
            overheat(player, wand);
            end(player, "overheat");
            return;
        }
        tether.start = beamStart(player);
        if (tether.ghost == null) {
            GhostEntity target = findTarget(player, tether.start);
            if (target != null) {
                tether.ghost = target.getUUID();
                tether.losLostTicks = 0;
                level.playSound(null, target.getX(), target.getY() + 0.6, target.getZ(), ModSounds.TETHER_LATCH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                ServerTrace.ghost("tether_start", target, line -> {
                    line.addProperty("player", player.getGameProfile().name());
                    line.addProperty("distance", ServerTrace.round(player.distanceTo(target)));
                });
            }
        }
        GhostEntity ghost = latched(level, tether);
        if (ghost == null) {
            if (tether.ghost != null) {
                end(player, "lost");
                return;
            }
            tether.end = freeEnd(player, tether.start);
            return;
        }
        String broken = breakReason(player, ghost, tether);
        if (broken != null) {
            end(player, broken);
            return;
        }
        tether.end = ghost.getBoundingBox().getCenter();
        ghost.drainByTether(PresenceRules.TETHER_DRAIN_PER_TICK);
        double distance = player.distanceTo(ghost);
        if (distance > PULL_DISTANCE) {
            Vec3 pull = ghost.position().subtract(player.position()).normalize().scale(PULL_PER_TICK);
            player.push(pull.x, pull.y * 0.5, pull.z);
            player.hurtMarked = true;
        }
    }

    /**
     * The player let go of use (or switched away).
     */
    public static void release(ServerPlayer player) {

        end(player, "released");
    }

    public static boolean isFiring(Player player) {

        Tether tether = TETHERS.get(player.getUUID());
        return tether != null && tether.lastFireTick >= player.level().getGameTime() - 1;
    }

    /**
     * Debug: keep {@code player} firing their held wand without holding use.
     */
    public static void setForced(ServerPlayer player, boolean forced) {

        if (forced) {
            FORCED.add(player.getUUID());
        } else {
            FORCED.remove(player.getUUID());
            end(player, "released");
        }
    }

    public static @Nullable Tether tetherOf(Player player) {

        return TETHERS.get(player.getUUID());
    }

    /**
     * Names of the players whose tethers are latched on {@code ghost}.
     */
    public static List<String> holdersOf(GhostEntity ghost) {

        List<String> names = new ArrayList<>();
        for (Tether tether : TETHERS.values()) {
            if (ghost.getUUID().equals(tether.ghost) && ghost.level() instanceof ServerLevel level
                    && level.getServer().getPlayerList().getPlayer(tether.player) instanceof ServerPlayer player) {
                names.add(player.getGameProfile().name());
            }
        }
        return names;
    }

    private static void end(ServerPlayer player, String reason) {

        // A broken tether stops forced firing too, as it stops a real player's use: firing on would re-fire a free
        // beam at once (two co-op beams on a just-captured ghost would cross and backfire).
        if (!reason.equals("released")) {
            FORCED.remove(player.getUUID());
        }
        Tether tether = TETHERS.remove(player.getUUID());
        if (tether == null) {
            return;
        }
        ServerTrace.event("tether_end", line -> {
            line.addProperty("player", player.getGameProfile().name());
            line.addProperty("ghost", String.valueOf(tether.ghost));
            line.addProperty("reason", reason);
        });
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof TetherWandItem && !reason.equals("released")) {
            player.stopUsingItem();
        }
    }

    private static void overheat(ServerPlayer player, ItemStack wand) {

        ServerLevel level = player.level();
        wand.set(ModDataComponents.WAND_HEAT.get(), wand.getOrDefault(ModDataComponents.WAND_HEAT.get(), WandHeat.COLD).overheat());
        Vec3 at = beamStart(player);
        level.playSound(null, at.x, at.y, at.z, ModSounds.TETHER_OVERHEAT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.02);
        ServerTrace.event("wand_overheat", line -> line.addProperty("player", player.getGameProfile().name()));
    }

    // ---- geometry ----

    /**
     * Where the beam leaves the wand: a little ahead of the eyes, low and to the right (the main hand).
     */
    public static Vec3 beamStart(Player player) {

        Vec3 look = player.getViewVector(1.0F);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        return player.getEyePosition().add(look.scale(0.6)).add(right.scale(0.3)).add(0, -0.25, 0);
    }

    /**
     * Where an unlatched beam ends: {@link #RANGE} toward the crosshair, or the first block that stops it. Like line
     * of sight, it passes doors, panes, fences and glass.
     */
    private static Vec3 freeEnd(Player player, Vec3 start) {

        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getViewVector(1.0F).scale(RANGE));
        Vec3 blocked = firstBlocked(player.level(), eye, far);
        return blocked == null ? far : blocked;
    }

    /**
     * The first visible ghost within {@link #LATCH_DISTANCE} of the line from the eyes toward the crosshair, nearest
     * along the beam.
     */
    private static @Nullable GhostEntity findTarget(ServerPlayer player, Vec3 start) {

        Vec3 eye = player.getEyePosition();
        Vec3 end = freeEnd(player, start);
        BeamMath.Point a = point(eye);
        BeamMath.Point b = point(end);
        GhostEntity best = null;
        double bestAlong = Double.MAX_VALUE;
        for (GhostEntity ghost : player.level().getEntitiesOfClass(GhostEntity.class, new AABB(eye, end).inflate(LATCH_DISTANCE + 1.0))) {
            if (!ghost.isAlive() || !GhostVisibility.canSee(player, ghost) || ghost.capturingTrap() != null) {
                continue;
            }
            Vec3 centre = ghost.getBoundingBox().getCenter();
            if (BeamMath.pointToSegment(point(centre), a, b) > LATCH_DISTANCE + ghost.getBbWidth() * 0.5) {
                continue;
            }
            double along = centre.subtract(eye).dot(end.subtract(eye).normalize());
            if (along >= 0 && along < bestAlong) {
                best = ghost;
                bestAlong = along;
            }
        }
        return best;
    }

    private static @Nullable GhostEntity latched(ServerLevel level, Tether tether) {

        if (tether.ghost == null) {
            return null;
        }
        Entity entity = level.getEntity(tether.ghost);
        return entity instanceof GhostEntity ghost && ghost.isAlive() ? ghost : null;
    }

    private static @Nullable String breakReason(ServerPlayer player, GhostEntity ghost, Tether tether) {

        if (ghost.capturingTrap() != null && ghost.isRemoved()) {
            return "captured";
        }
        if (player.distanceTo(ghost) > BREAK_RANGE) {
            return "range";
        }
        if (!GhostVisibility.canSee(player, ghost)) {
            return "unseen";
        }
        if (hasLineOfSight(player.level(), player.getEyePosition(), ghost.getBoundingBox().getCenter())) {
            tether.losLostTicks = 0;
        } else if (++tether.losLostTicks >= LOS_GRACE_TICKS) {
            return "line_of_sight";
        }
        return null;
    }

    /**
     * Whether nothing solid lies between two points, ignoring {@code ghost_passable} blocks (doors, panes, fences)
     * and glass ({@code #minecraft:impermeable}), which the player can see through.
     */
    public static boolean hasLineOfSight(Level level, Vec3 from, Vec3 to) {

        return firstBlocked(level, from, to) == null;
    }

    /**
     * The first point (in quarter-block steps) between two points inside a block that stops beams, or null.
     */
    private static @Nullable Vec3 firstBlocked(Level level, Vec3 from, Vec3 to) {

        Vec3 step = to.subtract(from);
        int steps = Math.max(1, (int) Math.ceil(step.length() * 4.0));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < steps; i++) {
            Vec3 at = from.add(step.scale((double) i / steps));
            pos.set(at.x, at.y, at.z);
            BlockState state = level.getBlockState(pos);
            if (GhostMovement.isPassable(state) || state.is(BlockTags.IMPERMEABLE)) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            for (AABB box : shape.toAabbs()) {
                if (box.move(pos).contains(at)) {
                    return at;
                }
            }
        }
        return null;
    }

    private static BeamMath.Point point(Vec3 vec) {

        return new BeamMath.Point(vec.x, vec.y, vec.z);
    }

    // ---- every server tick ----

    /**
     * After all players have ticked: drop tethers whose player stopped firing, fire forced ones, tell ghosts who holds
     * them, check for crossed beams, and send the beams to clients.
     */
    public static void tick(MinecraftServer server) {

        for (UUID id : List.copyOf(FORCED)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                FORCED.remove(id);
                continue;
            }
            ItemStack wand = heldWand(player);
            if (wand != null) {
                fire(player, wand);
            }
        }
        for (UUID id : List.copyOf(TETHERS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Tether tether = TETHERS.get(id);
            if (player == null) {
                TETHERS.remove(id);
            } else if (tether.lastFireTick < player.level().getGameTime()) {
                end(player, "stopped");
            }
        }
        Map<GhostEntity, List<Vec3>> held = new HashMap<>();
        for (Map.Entry<UUID, Tether> entry : TETHERS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            GhostEntity ghost = player == null ? null : latched(player.level(), entry.getValue());
            if (ghost != null) {
                held.computeIfAbsent(ghost, key -> new ArrayList<>()).add(player.position());
            }
        }
        for (GhostEntity ghost : TETHERED) {
            if (!held.containsKey(ghost)) {
                ghost.setTethers(List.of());
            }
        }
        held.forEach(GhostEntity::setTethers);
        TETHERED.clear();
        TETHERED.addAll(held.keySet());
        checkCrossings(server);
        broadcast(server);
    }

    public static @Nullable ItemStack heldWand(Player player) {

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof TetherWandItem) {
                return stack;
            }
        }
        return null;
    }

    private static void checkCrossings(MinecraftServer server) {

        List<Map.Entry<UUID, Tether>> beams = new ArrayList<>(TETHERS.entrySet());
        for (int i = 0; i < beams.size(); i++) {
            for (int j = i + 1; j < beams.size(); j++) {
                Tether a = beams.get(i).getValue();
                Tether b = beams.get(j).getValue();
                BeamMath.Point crossing = BeamMath.crossing(point(a.start), point(a.end), point(b.start), point(b.end));
                if (crossing == null) {
                    continue;
                }
                ServerPlayer first = server.getPlayerList().getPlayer(beams.get(i).getKey());
                ServerPlayer second = server.getPlayerList().getPlayer(beams.get(j).getKey());
                if (first != null && second != null && first.level() == second.level()) {
                    backfire(first, second, new Vec3(crossing.x(), crossing.y(), crossing.z()));
                    return;
                }
            }
        }
    }

    private static void backfire(ServerPlayer first, ServerPlayer second, Vec3 at) {

        ServerLevel level = first.level();
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, at.x, at.y, at.z, ModSounds.TETHER_BACKFIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        ServerTrace.event("backfire", line -> {
            line.addProperty("first", first.getGameProfile().name());
            line.addProperty("second", second.getGameProfile().name());
            line.addProperty("x", ServerTrace.round(at.x));
            line.addProperty("y", ServerTrace.round(at.y));
            line.addProperty("z", ServerTrace.round(at.z));
        });
        for (ServerPlayer player : List.of(first, second)) {
            ItemStack wand = heldWand(player);
            if (wand != null) {
                overheat(player, wand);
            }
            end(player, "backfire");
            Vec3 away = player.position().subtract(at).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            player.push(away.x * BACKFIRE_KNOCKBACK, 0.35, away.z * BACKFIRE_KNOCKBACK);
            player.hurtMarked = true;
            player.hurtServer(level, level.damageSources().magic(), BACKFIRE_DAMAGE);
        }
    }

    private static void broadcast(MinecraftServer server) {

        Map<ServerLevel, List<BeamStatePayload.Beam>> byLevel = new HashMap<>();
        for (Map.Entry<UUID, Tether> entry : TETHERS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            Tether tether = entry.getValue();
            GhostEntity ghost = latched(player.level(), tether);
            byLevel.computeIfAbsent(player.level(), key -> new ArrayList<>()).add(new BeamStatePayload.Beam(player.getId(),
                    ghost == null ? -1 : ghost.getId(), tether.end.x, tether.end.y, tether.end.z));
        }
        if (byLevel.isEmpty() && !beamsSentLastTick) {
            return;
        }
        beamsSentLastTick = !byLevel.isEmpty();
        for (ServerLevel level : server.getAllLevels()) {
            BeamStatePayload payload = new BeamStatePayload(byLevel.getOrDefault(level, List.of()));
            for (ServerPlayer player : level.players()) {
                Services.NETWORK.sendToPlayer(player, payload);
            }
        }
    }

    public static void clear() {

        TETHERS.clear();
        FORCED.clear();
        TETHERED.clear();
        beamsSentLastTick = false;
    }
}
