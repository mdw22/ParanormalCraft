package io.github.mdw22.paranormalcraft.familiar;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.binding.BoundJobs;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.journal.JournalEvent;
import io.github.mdw22.paranormalcraft.journal.JournalEvents;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Who has a Wisp Familiar out, and the rules that tie it to its vessel. One familiar per player; using another bound
 * vessel recalls the first. {@link #tick} recalls a wisp when its owner logs out, changes dimension, or no longer
 * has the vessel (dropped, stored or moved away); the loaders also call {@link #onLogout} and
 * {@link #onServerStopping}. The bound ghost never leaves the vessel, so a recall loses nothing.
 */
public final class Familiars {

    private static final int SWEEP_INTERVAL_TICKS = Balance.Familiar.LIGHT_SWEEP_TICKS;
    private static final Map<UUID, WispFamiliarEntity> OUT = new HashMap<>();
    /** Game time each player may guide again. */
    private static final Map<UUID, Long> GUIDE_READY = new HashMap<>();
    /** Finds the nearest Abandoned Cabin: vanilla's structure locate, capped at {@code GUIDE_RADIUS_CHUNKS}. */
    private static final BiFunction<ServerLevel, BlockPos, @Nullable BlockPos> VANILLA_LOCATE = (level, origin) ->
            level.findNearestMapStructure(ModStructures.ABANDONED_CABIN, origin, Balance.Familiar.GUIDE_RADIUS_CHUNKS, false);
    /** GameTest worlds generate no structures, so tests swap in their own locator; null uses vanilla's. */
    private static @Nullable BiFunction<ServerLevel, BlockPos, @Nullable BlockPos> locatorOverride;

    private Familiars() {
    }

    public static @Nullable WispFamiliarEntity of(ServerPlayer player) {

        return OUT.get(player.getUUID());
    }

    public static List<WispFamiliarEntity> all() {

        return List.copyOf(OUT.values());
    }

    /**
     * Use of a Bound Spirit Vessel: summons its wisp, or recalls it if it is out.
     */
    public static void toggle(ServerPlayer player, ItemStack vessel) {

        CapturedGhost ghost = vessel.get(ModDataComponents.CAPTURED_GHOST.get());
        if (ghost == null) {
            return;
        }
        WispFamiliarEntity out = OUT.get(player.getUUID());
        if (out != null && ghost.id().equals(out.ghost())) {
            recall(out, "used");
            return;
        }
        summon(player, ghost);
    }

    /**
     * Summons the wisp for {@code ghost} next to {@code player}, recalling any other one they have out.
     */
    public static @Nullable WispFamiliarEntity summon(ServerPlayer player, CapturedGhost ghost) {

        BoundJobs.Job job = BoundJobs.jobFor(ghost.entityType());
        if (job == null || !job.id().equals(BoundJobs.WISP_FAMILIAR)) {
            return null;
        }
        WispFamiliarEntity previous = OUT.get(player.getUUID());
        if (previous != null) {
            recall(previous, "replaced");
        }
        ServerLevel level = player.level();
        WispFamiliarEntity wisp = ModEntities.WISP_FAMILIAR.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (wisp == null) {
            return null;
        }
        wisp.bindTo(player, ghost.id(), ghost.name());
        Vec3 at = player.position().add(0.0, Balance.Familiar.SHOULDER_HEIGHT, 0.0);
        wisp.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        level.addFreshEntity(wisp);
        OUT.put(player.getUUID(), wisp);
        level.playSound(null, at.x, at.y, at.z, ModSounds.WISP_SUMMON.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        ServerTrace.event("familiar_summon", line -> {
            line.addProperty("owner", player.getGameProfile().name());
            line.addProperty("name", ghost.name());
            line.addProperty("job", job.id());
        });
        JournalEvents.fire(player, JournalEvent.FAMILIAR_SUMMONED, ghost.entityType());
        return wisp;
    }

    /**
     * Sends the wisp back into its vessel: removes its light and the entity.
     */
    public static void recall(WispFamiliarEntity wisp, String reason) {

        if (wisp.owner() != null && OUT.get(wisp.owner()) == wisp) {
            OUT.remove(wisp.owner());
        }
        if (wisp.level() instanceof ServerLevel level) {
            wisp.removeLight(level, "recall");
            level.playSound(null, wisp.getX(), wisp.getY(), wisp.getZ(), ModSounds.WISP_RECALL.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        traceRecall(wisp, reason);
        wisp.discard();
    }

    /** From the entity's own removal: forgets a wisp that went without a recall (an unload, a kill command). */
    static void forget(WispFamiliarEntity wisp, @Nullable String reason) {

        if (wisp.owner() != null && OUT.get(wisp.owner()) == wisp) {
            OUT.remove(wisp.owner());
            traceRecall(wisp, reason == null ? "removed" : reason);
        }
    }

    private static void traceRecall(WispFamiliarEntity wisp, String reason) {

        ServerTrace.event("familiar_recall", line -> {
            line.addProperty("name", wisp.ghostName());
            line.addProperty("reason", reason);
        });
    }

    /**
     * Sneak + use while the wisp is out: it heads toward the nearest Abandoned Cabin within
     * {@code GUIDE_RADIUS_CHUNKS} and waits, or shakes "no". Returns the cabin found, if any.
     */
    public static @Nullable BlockPos guide(ServerPlayer player) {

        WispFamiliarEntity wisp = OUT.get(player.getUUID());
        if (wisp == null) {
            return null;
        }
        ServerLevel level = player.level();
        long now = level.getGameTime();
        long ready = GUIDE_READY.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (now < ready) {
            long seconds = (ready - now + 19) / 20;
            player.sendOverlayMessage(Component.translatable("message.paranormalcraft.wisp_guide_cooldown", seconds));
            ServerTrace.event("wisp_guide", line -> {
                line.addProperty("owner", player.getGameProfile().name());
                line.addProperty("result", "cooldown");
            });
            return null;
        }
        long start = System.nanoTime();
        BlockPos cabin = (locatorOverride != null ? locatorOverride : VANILLA_LOCATE).apply(level, player.blockPosition());
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        if (cabin != null) {
            wisp.guideToward(player, Vec3.atCenterOf(cabin));
            GUIDE_READY.put(player.getUUID(), now + Balance.Familiar.GUIDE_COOLDOWN_TICKS);
        } else {
            wisp.shakeNo();
            level.playSound(null, wisp.getX(), wisp.getY(), wisp.getZ(), ModSounds.WISP_NO.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            GUIDE_READY.put(player.getUUID(), now + Balance.Familiar.GUIDE_FAIL_COOLDOWN_TICKS);
        }
        ServerTrace.event("wisp_guide", line -> {
            line.addProperty("owner", player.getGameProfile().name());
            line.addProperty("result", cabin == null ? "none" : "found");
            if (cabin != null) {
                line.addProperty("cabin", cabin.toShortString());
                line.addProperty("distance", Math.round(Math.sqrt(cabin.distSqr(player.blockPosition()))));
            }
            line.addProperty("locateMs", ServerTrace.round(millis));
        });
        return cabin;
    }

    /** Tests only: replaces the cabin locate ({@code null} restores vanilla's). */
    public static void setLocatorOverride(@Nullable BiFunction<ServerLevel, BlockPos, @Nullable BlockPos> locator) {

        locatorOverride = locator;
    }

    /** Debug and tests: lets the player guide again at once. */
    public static void resetGuideCooldown(ServerPlayer player) {

        GUIDE_READY.remove(player.getUUID());
    }

    // ---- the vessel link ----

    /**
     * Whether the player's inventory (or the stack on their cursor) still holds the vessel of {@code ghost}.
     */
    public static boolean hasVessel(ServerPlayer player, UUID ghost) {

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (holds(inventory.getItem(slot), ghost)) {
                return true;
            }
        }
        return holds(player.containerMenu.getCarried(), ghost);
    }

    private static boolean holds(ItemStack stack, UUID ghost) {

        CapturedGhost bound = stack.get(ModDataComponents.CAPTURED_GHOST.get());
        return bound != null && !stack.is(io.github.mdw22.paranormalcraft.registry.ModItems.FILLED_GHOST_TRAP.get()) && ghost.equals(bound.id());
    }

    /** From {@code CommonEvents.onServerTick}. */
    public static void tick(MinecraftServer server) {

        for (WispFamiliarEntity wisp : List.copyOf(OUT.values())) {
            if (wisp.isRemoved()) {
                OUT.values().remove(wisp);
                continue;
            }
            ServerPlayer owner = wisp.owner() == null ? null : server.getPlayerList().getPlayer(wisp.owner());
            if (owner == null) {
                recall(wisp, "logout");
            } else if (owner.level() != wisp.level()) {
                recall(wisp, "dimension");
            } else if (wisp.ghost() == null || !hasVessel(owner, wisp.ghost())) {
                recall(wisp, "vessel_gone");
            }
        }
        if (server.getTickCount() % SWEEP_INTERVAL_TICKS == 0) {
            Set<String> owned = new HashSet<>();
            for (WispFamiliarEntity wisp : OUT.values()) {
                if (wisp.light() != null) {
                    owned.add(wisp.level().dimension().identifier() + " " + wisp.light().asLong());
                }
            }
            int removed = WispLights.sweepLoaded(server, owned);
            if (removed > 0) {
                ServerTrace.event("wisp_light_sweep", line -> {
                    line.addProperty("loaded", true);
                    line.addProperty("removed", removed);
                });
            }
        }
    }

    /** Loader hook: a player left. */
    public static void onLogout(ServerPlayer player) {

        WispFamiliarEntity wisp = OUT.get(player.getUUID());
        if (wisp != null) {
            recall(wisp, "logout");
        }
    }

    /** Loader hook: the server is stopping, levels still loaded, so every light can still be removed. */
    public static void onServerStopping(MinecraftServer server) {

        for (WispFamiliarEntity wisp : List.copyOf(OUT.values())) {
            recall(wisp, "server_stop");
        }
    }

    /** Loader hook: the server has started; clears any light a crash left behind. */
    public static int onServerStarted(MinecraftServer server) {

        return WispLights.sweepAll(server);
    }

    public static void clear() {

        OUT.clear();
        GUIDE_READY.clear();
        locatorOverride = null;
    }

    /** For the debug dump. */
    public static Map<UUID, WispFamiliarEntity> out() {

        return Map.copyOf(OUT);
    }
}
