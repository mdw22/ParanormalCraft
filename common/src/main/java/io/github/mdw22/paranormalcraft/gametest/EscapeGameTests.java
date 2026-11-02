package io.github.mdw22.paranormalcraft.gametest;

import com.google.gson.JsonObject;
import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.CapturedGhosts;
import io.github.mdw22.paranormalcraft.capture.LooseGhosts;
import io.github.mdw22.paranormalcraft.capture.TrapDecay;
import io.github.mdw22.paranormalcraft.capture.TrapIntegrity;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlockEntity;
import io.github.mdw22.paranormalcraft.debug.DebugScenes;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Attachment;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostFollowGoal;
import io.github.mdw22.paranormalcraft.haunting.HauntingAction;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * GameTests for Milestone 2 part D: Filled Trap integrity (a trap in a chest, a placed trap, containment stopping the
 * decay, the tooltip and item bar), Containment breaches (timing, order, refuelling), Agitated ghosts, and attachment
 * (attach and follow, no haunting while following, settling at the respawn point and haunting there, waiting across a
 * dimension change, wards, recapture, the seeded failed-capture roll), and getting away after an escape (it shows
 * itself where it got out, flees, ends up behind the player, and neither jumps nor settles before that). Own batch
 * ({@code escape}).
 */
public final class EscapeGameTests {

    private static final BlockPos CENTRE = new BlockPos(20, 1, 20);
    private static final int ESCAPE_WATCH_TICKS = 600;
    /** More than this in one tick is a jump: a fleeing ghost covers well under a block. */
    private static final double MAX_STEP = 1.5;

    private EscapeGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("trap_in_chest_escapes_at_zero", EscapeGameTests::trapInChestEscapes);
        RegistrationHelper.testFunction("placed_trap_escapes_at_zero", EscapeGameTests::placedTrapEscapes);
        RegistrationHelper.testFunction("containment_stops_trap_decay", EscapeGameTests::containmentStopsDecay);
        RegistrationHelper.testFunction("trap_integrity_tooltip_and_bar", EscapeGameTests::tooltipAndBar);
        RegistrationHelper.testFunction("containment_breach_cycle", EscapeGameTests::breachCycle);
        RegistrationHelper.testFunction("agitated_ghost_behaviour", EscapeGameTests::agitated);
        RegistrationHelper.testFunction("escaped_ghost_attaches_and_follows", EscapeGameTests::attachesAndFollows);
        RegistrationHelper.testFunction("attached_ghost_settles_at_respawn", EscapeGameTests::settlesAtRespawn);
        RegistrationHelper.testFunction("attached_ghost_waits_across_dimensions", EscapeGameTests::waitsAcrossDimensions);
        RegistrationHelper.testFunction("ward_stops_haunting_at_settled_home", EscapeGameTests::wardAtSettledHome);
        RegistrationHelper.testFunction("recapture_clears_attachment", EscapeGameTests::recaptureClears);
        RegistrationHelper.testFunction("failed_capture_attach_roll", EscapeGameTests::failedCaptureAttach);
        RegistrationHelper.testFunction("agitated_ghost_notices_player", EscapeGameTests::agitatedNotice);
        RegistrationHelper.testFunction("trap_escape_flees_visibly", EscapeGameTests::trapEscapeFlees);
        RegistrationHelper.testFunction("breach_escape_flees_visibly", EscapeGameTests::breachEscapeFlees);
        RegistrationHelper.testFunction("escape_settles_only_after_getting_away", EscapeGameTests::settlesAfterGettingAway);
        RegistrationHelper.testFunction("attached_ghost_teleports_close_behind", EscapeGameTests::teleportsCloseBehind);
        RegistrationHelper.testFunction("scene_forgets_home_base_respawn", EscapeGameTests::sceneForgetsRespawn);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    // TODO(26.3): makeMockServerPlayerInLevel is deprecated; switch once a replacement exists.
    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos relative) {

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
        player.teleportTo(at.x, at.y, at.z);
        return player;
    }

    private static void remove(ServerPlayer player) {

        player.level().getServer().getPlayerList().remove(player);
    }

    private static ItemStack filledTrap(GameTestHelper helper, float integrity) {

        ServerLevel level = helper.getLevel();
        ItemStack stack = CapturedGhosts.filledTrap(level, helper.absolutePos(CENTRE), "test");
        stack.set(ModDataComponents.TRAP_SEAL.get(), TrapIntegrity.at(level.getGameTime(), integrity, 24_000));
        return stack;
    }

    private static List<GhostEntity> ghostsNear(GameTestHelper helper, BlockPos relative, double radius) {

        return helper.getLevel().getEntitiesOfClass(GhostEntity.class, new AABB(helper.absolutePos(relative)).inflate(radius));
    }

    private static boolean advancementDone(ServerPlayer player, String path) {

        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Constants.id(path));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    // ---- integrity ----

    private static void trapInChestEscapes(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, CENTRE.south(3));
        BlockPos chestAt = new BlockPos(10, 1, 10);
        helper.setBlock(chestAt, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestAt, ChestBlockEntity.class);
        chest.setItem(4, filledTrap(helper, 0.0F));
        chest.setItem(5, filledTrap(helper, 80.0F));
        BlockPos absolute = helper.absolutePos(chestAt);
        // What the decay sweep does when it reaches this chunk.
        TrapDecay.scanChunk(level, absolute.getX() >> 4, absolute.getZ() >> 4, 1.0);
        check(helper, chest.getItem(4).is(ModItems.GHOST_TRAP.get()) && !chest.getItem(4).has(ModDataComponents.CAPTURED_GHOST.get()),
                "the broken trap wasn't left empty: %s", chest.getItem(4));
        check(helper, TrapDecay.isFilledTrap(chest.getItem(5)), "the healthy trap escaped too");
        List<GhostEntity> loose = ghostsNear(helper, chestAt, 3.0);
        check(helper, loose.size() == 1, "%d ghosts by the chest, expected 1", loose.size());
        GhostEntity ghost = loose.getFirst();
        check(helper, ghost.isAgitated(), "the escaped ghost isn't Agitated");
        check(helper, ghost.getPresence() >= 19.9F, "the escaped ghost isn't at full Presence (%.1f)", ghost.getPresence());
        check(helper, ghost.attachment().following() && player.getUUID().equals(ghost.attachment().player()), "it didn't attach to the nearby player");
        ghost.discard();
        remove(player);
        helper.succeed();
    }

    private static void placedTrapEscapes(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, CENTRE.south(3));
        helper.setBlock(CENTRE, ModBlocks.FILLED_GHOST_TRAP.get());
        FilledGhostTrapBlockEntity trap = (FilledGhostTrapBlockEntity) helper.getBlockEntity(CENTRE, FilledGhostTrapBlockEntity.class);
        CapturedGhost ghost = CapturedGhosts.fresh(level, helper.absolutePos(CENTRE), "test");
        trap.setContents(ghost);
        trap.setSeal(TrapIntegrity.at(level.getGameTime(), 0.0F, 24_000));
        helper.runAfterDelay(3, () -> {
            check(helper, helper.getBlockState(CENTRE).is(ModBlocks.GHOST_TRAP.get()), "the placed trap wasn't left empty");
            List<GhostEntity> loose = ghostsNear(helper, CENTRE, 3.0);
            check(helper, loose.size() == 1 && loose.getFirst().isAgitated(), "no Agitated ghost by the trap");
            loose.forEach(GhostEntity::discard);
            remove(player);
            helper.succeed();
        });
    }

    private static void containmentStopsDecay(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        helper.setBlock(CENTRE, ModBlocks.CONTAINMENT_UNIT.get());
        ContainmentUnitBlockEntity unit = (ContainmentUnitBlockEntity) helper.getBlockEntity(CENTRE, ContainmentUnitBlockEntity.class);
        unit.setCharge(100);
        unit.setItem(ContainmentUnitBlockEntity.INPUT_SLOT, filledTrap(helper, 5.0F));
        helper.runAfterDelay(3, () -> {
            check(helper, unit.stored().size() == 1, "the trap wasn't loaded");
            // Broken out again, the ghost comes back in a freshly sealed trap: storage didn't decay it.
            helper.setBlock(CENTRE, Blocks.AIR);
            List<ItemStack> traps = new ArrayList<>();
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(helper.absolutePos(CENTRE)).inflate(3))
                    .forEach(item -> {
                        if (TrapDecay.isFilledTrap(item.getItem())) {
                            traps.add(item.getItem());
                        }
                        item.discard();
                    });
            check(helper, traps.size() == 1, "%d filled traps dropped, expected 1", traps.size());
            float integrity = TrapIntegrity.integrity(traps.getFirst().get(ModDataComponents.TRAP_SEAL.get()), level.getGameTime());
            check(helper, integrity > 99.0F, "the dropped trap is at %.1f%%, expected a fresh seal", integrity);
            helper.succeed();
        });
    }

    private static void tooltipAndBar(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ItemStack trap = filledTrap(helper, 64.0F);
        float integrity = TrapIntegrity.integrity(trap.get(ModDataComponents.TRAP_SEAL.get()), level.getGameTime());
        check(helper, Math.abs(integrity - 64.0F) < 0.01F, "integrity %.2f, expected 64", integrity);
        // The bar and tooltip read the client's clock; point it at this level for the check.
        TrapIntegrity.setClientClock(level::getGameTime);
        try {
            check(helper, trap.isBarVisible(), "no item bar");
            check(helper, trap.getBarWidth() == Math.round(13 * 0.64F), "bar width %d, expected %d", trap.getBarWidth(), Math.round(13 * 0.64F));
            check(helper, trap.getBarColor() == TrapIntegrity.barColour(64.0F), "bar colour %06x", trap.getBarColor());
            List<String> lines = new ArrayList<>();
            trap.getItem().appendHoverText(trap, net.minecraft.world.item.Item.TooltipContext.of(level), TooltipDisplay.DEFAULT,
                    line -> lines.add(line.getString()), TooltipFlag.NORMAL);
            check(helper, lines.contains("Integrity 64%"), "tooltip lines: %s", lines);
        } finally {
            TrapIntegrity.setClientClock(() -> 0L);
        }
        helper.succeed();
    }

    // ---- breach ----

    private static void breachCycle(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, CENTRE.south(4));
        helper.setBlock(CENTRE, ModBlocks.CONTAINMENT_UNIT.get());
        ContainmentUnitBlockEntity unit = (ContainmentUnitBlockEntity) helper.getBlockEntity(CENTRE, ContainmentUnitBlockEntity.class);
        unit.fillForDebug(level, 3);
        List<String> names = unit.stored().stream().map(ghost -> ghost.ghost().name()).toList();
        // The first ghost is processing: it should go last.
        unit.setCharge(20);
        check(helper, unit.queueProcess(0, player), "couldn't queue (set-up)");
        unit.setCharge(0);
        helper.runAfterDelay(2, () -> {
            check(helper, unit.isUnstable() && !unit.inBreach(), "not Unstable (or already in Breach) at charge 0");
            unit.debugSetUnstableTicks(Balance.Escape.BREACH_AFTER_TICKS - 3);
        });
        helper.runAfterDelay(3, () -> check(helper, !unit.inBreach(), "Breach before a minute Unstable"));
        helper.runAfterDelay(6, () -> {
            check(helper, unit.inBreach(), "no Breach after a minute Unstable");
            check(helper, advancementDone(player, "main/breach"), "Breach! not awarded to a player 4 blocks away");
            unit.debugAdvanceBreach(10_000);
        });
        helper.runAfterDelay(8, () -> {
            check(helper, unit.stored().size() == 2, "%d ghosts left after the first escape, expected 2", unit.stored().size());
            check(helper, unit.stored().stream().noneMatch(ghost -> ghost.ghost().name().equals(names.get(1))),
                    "the wrong ghost escaped first (expected %s, the first not processing)", names.get(1));
            check(helper, unit.stored().stream().anyMatch(ghost -> ghost.ghost().name().equals(names.getFirst())), "the processing ghost escaped early");
            List<GhostEntity> loose = ghostsNear(helper, CENTRE, 4.0);
            check(helper, loose.size() == 1 && loose.getFirst().isAgitated(), "no Agitated ghost out of the unit");
            check(helper, loose.getFirst().getHome() != null && loose.getFirst().getHome().equals(helper.absolutePos(CENTRE)), "the loose ghost isn't homed at the unit");
            loose.forEach(GhostEntity::discard);
            unit.setCharge(20);
        });
        helper.runAfterDelay(11, () -> {
            check(helper, !unit.isUnstable() && !unit.inBreach(), "refuelling didn't end the Breach");
            unit.debugAdvanceBreach(10_000);
        });
        helper.runAfterDelay(14, () -> {
            check(helper, unit.stored().size() == 2, "a ghost escaped after the Breach ended");
            remove(player);
            helper.succeed();
        });
    }

    // ---- Agitated ----

    private static void agitated(GameTestHelper helper) {

        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE.above());
        ghost.agitate(6);
        check(helper, ghost.isAgitated() && ghost.agitatedTicks() == 6, "not Agitated");
        check(helper, !ghost.goalAllowed(GhostEntity.ForcedGoal.CURIOUS), "an Agitated ghost may be curious");
        check(helper, ghost.goalAllowed(GhostEntity.ForcedGoal.WANDER), "an Agitated ghost can't wander");
        helper.runAfterDelay(10, () -> {
            check(helper, !ghost.isAgitated(), "still Agitated after its time");
            check(helper, ghost.goalAllowed(GhostEntity.ForcedGoal.CURIOUS), "still not curious once calm");
            ghost.discard();
            helper.succeed();
        });
    }

    // ---- attachment ----

    private static GhostEntity looseGhost(GameTestHelper helper, BlockPos relative) {

        ServerLevel level = helper.getLevel();
        CapturedGhost captured = CapturedGhosts.fresh(level, helper.absolutePos(relative), "test");
        GhostEntity ghost = LooseGhosts.escape(level, captured, helper.absoluteVec(Vec3.atBottomCenterOf(relative)), helper.absolutePos(relative), "test");
        if (ghost == null) {
            throw helper.assertionException(Component.literal("couldn't make a loose ghost"));
        }
        // Past the getting-away part, which has its own tests.
        ghost.attachment().endEscape("test");
        return ghost;
    }

    private static void attachesAndFollows(GameTestHelper helper) {

        ServerPlayer far = player(helper, new BlockPos(2, 1, 2));
        ServerPlayer near = player(helper, CENTRE.south(5));
        GhostEntity ghost = looseGhost(helper, CENTRE);
        check(helper, ghost.attachment().following() && near.getUUID().equals(ghost.attachment().player()), "it didn't attach to the nearest player");
        check(helper, !ghost.goalAllowed(GhostEntity.ForcedGoal.HOME) && !ghost.goalAllowed(GhostEntity.ForcedGoal.WANDER), "its own goals still run");
        helper.runAfterDelay(60, () -> {
            check(helper, !ghost.haunting().schedule().isScheduled(), "it scheduled a haunting while following");
            check(helper, !ghost.isManifesting(), "it manifested while following");
            double distance = ghost.distanceTo(near);
            check(helper, distance >= Attachment.FOLLOW_MIN - 4.0 && distance <= Attachment.FOLLOW_TELEPORT, "follows at %.1f blocks", distance);
            ghost.discard();
            remove(far);
            remove(near);
            helper.succeed();
        });
    }

    // ---- getting away ----

    /**
     * Watches a ghost that just escaped from {@code at} for 30 s: never a jump, shown to everyone at first, near the
     * spot after a second, and by the end out of its flight and 12–24 blocks behind the player, with no catch-up teleport.
     * The clock starts when {@code loose} first finds it. Call this from the test function, not from a delayed task: the
     * framework can't take a new task while it runs those.
     */
    private static void watchEscape(GameTestHelper helper, ServerPlayer player, Supplier<@Nullable GhostEntity> loose, Vec3 at) {

        ServerLevel level = helper.getLevel();
        long start = level.getGameTime();
        GhostEntity[] found = {null};
        Vec3[] last = {null};
        int[] elapsed = {0};
        helper.onEachTick(() -> {
            if (found[0] == null) {
                found[0] = loose.get();
                if (found[0] == null) {
                    return;
                }
                last[0] = found[0].position();
                check(helper, found[0].attachment().following() && player.getUUID().equals(found[0].attachment().player()), "it didn't attach to the player");
                check(helper, found[0].isManifesting() && found[0].isAgitated(), "it isn't shown Agitated at the moment it escapes");
            }
            GhostEntity ghost = found[0];
            int tick = ++elapsed[0];
            if (tick > ESCAPE_WATCH_TICKS) {
                return;
            }
            double step = ghost.position().distanceTo(last[0]);
            last[0] = ghost.position();
            check(helper, step < MAX_STEP, "it jumped %.1f blocks on tick %d", step, tick);
            if (tick == 20) {
                check(helper, ghost.position().distanceTo(at) <= 6.0, "%.1f blocks from where it got out after 20 ticks", ghost.position().distanceTo(at));
            }
            if (tick == Balance.Escape.ESCAPE_MANIFEST_TICKS - 5) {
                check(helper, ghost.isManifesting(), "no longer shown after %d ticks", tick);
            }
            if (tick == Balance.Escape.ESCAPE_MANIFEST_TICKS + 5) {
                check(helper, !ghost.isManifesting(), "still shown after %d ticks", tick);
            }
            if (tick == ESCAPE_WATCH_TICKS) {
                List<JsonObject> trace = ServerTrace.since(start);
                check(helper, count(trace, "attach_teleport", ghost) == 0, "a catch-up teleport with the player within 30 blocks");
                check(helper, count(trace, "attach_flee_end", ghost) == 1, "it never finished fleeing");
                double distance = ghost.distanceTo(player);
                check(helper, ghost.attachment().escape() == Attachment.Escape.NONE, "still getting away (%s) after 30 s", ghost.attachment().escape());
                check(helper, distance >= Attachment.FOLLOW_MIN && distance <= Attachment.FOLLOW_MAX, "%.1f blocks from the player after 30 s", distance);
                check(helper, GhostFollowGoal.behind(player.position(), player.getViewVector(1.0F), ghost.position()), "not behind the player after 30 s");
                ghost.discard();
                remove(player);
                ServerTrace.stopBuffering();
                helper.succeed();
            }
        });
    }

    private static long count(List<JsonObject> trace, String type, GhostEntity ghost) {

        String id = ghost.getUUID().toString();
        return trace.stream().filter(line -> line.get("type").getAsString().equals(type) && line.has("ghost") && line.get("ghost").getAsString().equals(id)).count();
    }

    /** A player at the middle of the arena, looking along {@code yaw}. */
    private static ServerPlayer playerFacing(GameTestHelper helper, float yaw) {

        ServerPlayer player = player(helper, CENTRE);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        return player;
    }

    private static void trapEscapeFlees(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerTrace.startBuffering(level.getServer());
        // Facing the trap, so the ghost flees in front of them and has to come round.
        ServerPlayer player = playerFacing(helper, 0.0F);
        BlockPos trap = CENTRE.south(5);
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(trap));
        TrapDecay.escape(level, CapturedGhosts.fresh(level, helper.absolutePos(trap), "test"), at, "placed");
        List<GhostEntity> loose = ghostsNear(helper, trap, 3.0);
        check(helper, loose.size() == 1, "%d ghosts by the trap, expected 1", loose.size());
        watchEscape(helper, player, loose::getFirst, at);
    }

    private static void breachEscapeFlees(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerTrace.startBuffering(level.getServer());
        ServerPlayer player = playerFacing(helper, -90.0F);
        BlockPos at = CENTRE.east(5);
        helper.setBlock(at, ModBlocks.CONTAINMENT_UNIT.get());
        ContainmentUnitBlockEntity unit = (ContainmentUnitBlockEntity) helper.getBlockEntity(at, ContainmentUnitBlockEntity.class);
        unit.fillForDebug(level, 1);
        unit.setCharge(0);
        unit.debugSetUnstableTicks(Balance.Escape.BREACH_AFTER_TICKS);
        helper.runAfterDelay(3, () -> {
            check(helper, unit.inBreach(), "no Breach (set-up)");
            unit.debugAdvanceBreach(10_000);
        });
        watchEscape(helper, player, () -> {
            List<GhostEntity> loose = ghostsNear(helper, at, 4.0);
            check(helper, loose.size() <= 1, "%d ghosts by the unit, expected 1", loose.size());
            return loose.isEmpty() ? null : loose.getFirst();
        }, helper.absoluteVec(Vec3.atBottomCenterOf(at)));
    }

    /** The bug from the update12 playtest: with a respawn point in range, the ghost was at the bed on its first check. */
    private static void settlesAfterGettingAway(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerTrace.startBuffering(level.getServer());
        ServerPlayer player = playerFacing(helper, 180.0F);
        BlockPos bed = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(level.dimension(), bed), 0.0F, 0.0F), true), false);
        BlockPos trap = CENTRE.south(5);
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(trap));
        long start = level.getGameTime();
        TrapDecay.escape(level, CapturedGhosts.fresh(level, helper.absolutePos(trap), "test"), at, "placed");
        GhostEntity ghost = ghostsNear(helper, trap, 3.0).getFirst();
        helper.runAfterDelay(25, () -> {
            check(helper, ghost.attachment().following(), "it settled before getting away");
            check(helper, ghost.position().distanceTo(at) <= 6.0, "%.1f blocks from where it got out after 25 ticks", ghost.position().distanceTo(at));
        });
        helper.succeedWhen(() -> {
            check(helper, !ghost.attachment().attached(), "not settled yet");
            check(helper, bed.equals(ghost.getHome()), "home is %s, expected the respawn point %s", ghost.getHome(), bed);
            JsonObject settle = ServerTrace.since(start).stream().filter(line -> line.get("type").getAsString().equals("attach_settle")
                    && line.get("ghost").getAsString().equals(ghost.getUUID().toString())).findFirst().orElse(null);
            check(helper, settle != null && settle.get("cue").getAsBoolean(), "no \"Something settles in\" cue in the trace");
            ghost.discard();
            remove(player);
            ServerTrace.stopBuffering();
        });
    }

    private static void teleportsCloseBehind(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerTrace.startBuffering(level.getServer());
        ServerPlayer player = player(helper, CENTRE.south(5));
        GhostEntity ghost = looseGhost(helper, CENTRE);
        // Well past the catch-up range, with the chunks round the player loaded.
        Vec3 far = player.position().add(70.0, 0.0, 0.0);
        int cx = BlockPos.containing(far).getX() >> 4;
        int cz = BlockPos.containing(far).getZ() >> 4;
        // Only the ones this test forces get released: the rest may be under a neighbouring test.
        List<int[]> forced = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (level.setChunkForced(cx + dx, cz + dz, true)) {
                    forced.add(new int[] {cx + dx, cz + dz});
                }
            }
        }
        long start = level.getGameTime();
        player.teleportTo(far.x, far.y, far.z);
        helper.succeedWhen(() -> {
            JsonObject jump = ServerTrace.since(start).stream().filter(line -> line.get("type").getAsString().equals("attach_teleport")).findFirst().orElse(null);
            check(helper, jump != null, "no catch-up teleport yet");
            check(helper, jump.get("from").getAsDouble() > Attachment.FOLLOW_TELEPORT, "it jumped from only %.1f blocks", jump.get("from").getAsDouble());
            check(helper, jump.get("to").getAsDouble() <= Attachment.FOLLOW_MAX + 2.0, "it jumped to %.1f blocks from the player", jump.get("to").getAsDouble());
            ghost.discard();
            remove(player);
            forced.forEach(chunk -> level.setChunkForced(chunk[0], chunk[1], false));
            ServerTrace.stopBuffering();
        });
    }

    private static void sceneForgetsRespawn(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, CENTRE);
        ServerPlayer.RespawnConfig bed = new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(level.dimension(), DebugScenes.HOME_BASE_BED), 0.0F, 0.0F), true);
        player.setRespawnPosition(bed, false);
        DebugScenes.forgetSceneRespawn(player, "home_base");
        check(helper, player.getRespawnConfig() != null, "home_base lost its own respawn point");
        DebugScenes.forgetSceneRespawn(player, "containment_lab");
        check(helper, player.getRespawnConfig() == null, "another scene kept the home_base respawn point");
        BlockPos own = helper.absolutePos(CENTRE);
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(level.dimension(), own), 0.0F, 0.0F), true), false);
        DebugScenes.forgetSceneRespawn(player, "containment_lab");
        check(helper, player.getRespawnConfig() != null, "a scene cleared a respawn point of the player's own");
        remove(player);
        helper.succeed();
    }

    private static void settlesAtRespawn(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, CENTRE.south(5));
        // The bed is near the player, so the settled ghost (which slips straight home) is in haunting range of them.
        BlockPos bed = helper.absolutePos(new BlockPos(16, 1, 28));
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(level.dimension(), bed), 0.0F, 0.0F), true), false);
        GhostEntity ghost = looseGhost(helper, CENTRE);
        helper.runAfterDelay(25, () -> {
            check(helper, !ghost.attachment().attached(), "still attached near the respawn point");
            check(helper, bed.equals(ghost.getHome()), "home is %s, expected the respawn point %s", ghost.getHome(), bed);
            check(helper, player.getUUID().equals(ghost.attachment().settledOn()), "not recorded as settled on the player");
            check(helper, advancementDone(player, "main/uninvited_guest"), "Uninvited Guest not awarded");
        });
        helper.runAfterDelay(30, () -> {
            check(helper, ghost.haunting().schedule().isScheduled(), "it doesn't haunt its new home");
            ghost.discard();
            remove(player);
            helper.succeed();
        });
    }

    private static void waitsAcrossDimensions(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, CENTRE.south(5));
        GhostEntity ghost = looseGhost(helper, CENTRE);
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        check(helper, nether != null, "no Nether (set-up)");
        Vec3 back = player.position();
        helper.runAfterDelay(2, () -> player.teleportTo(nether, 0.5, 100, 0.5, Set.of(), 0.0F, 0.0F, true));
        helper.runAfterDelay(25, () -> {
            check(helper, ghost.attachment().mode() == Attachment.Mode.WAITING, "mode %s while its player is in the Nether", ghost.attachment().mode());
            check(helper, ghost.level() == level, "the ghost changed dimension");
            player.teleportTo(level, back.x, back.y, back.z, Set.of(), 0.0F, 0.0F, true);
        });
        helper.runAfterDelay(50, () -> {
            check(helper, ghost.attachment().following(), "it didn't resume once its player came back (mode %s)", ghost.attachment().mode());
            ghost.discard();
            remove(player);
            helper.succeed();
        });
    }

    private static void wardAtSettledHome(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, CENTRE.south(3));
        GhostEntity ghost = looseGhost(helper, CENTRE);
        ghost.attachment().settle(level, helper.absolutePos(CENTRE));
        check(helper, !ghost.attachment().attached(), "still attached after settling");
        helper.setBlock(CENTRE.east(6), ModBlocks.WARD_LANTERN.get());
        check(helper, ghost.haunting().force(level, HauntingAction.FOOTSTEPS).isEmpty(), "it haunted its settled home inside a ward");
        ghost.discard();
        remove(player);
        helper.succeed();
    }

    private static void recaptureClears(GameTestHelper helper) {

        ServerPlayer player = player(helper, CENTRE.south(4));
        GhostEntity ghost = looseGhost(helper, CENTRE);
        check(helper, ghost.attachment().following(), "set-up: not following");
        ghost.setPresence(helper.getLevel(), 4.0F);
        ghost.setNoAi(true);
        ghost.snapTo(helper.absoluteVec(new Vec3(20.5, 1.2, 20.5)));
        helper.setBlock(CENTRE, ModBlocks.GHOST_TRAP.get());
        GhostTrapBlockEntity trap = (GhostTrapBlockEntity) helper.getBlockEntity(CENTRE, GhostTrapBlockEntity.class);
        trap.setRollOverride(() -> 0.0);
        helper.succeedWhen(() -> {
            check(helper, helper.getBlockState(CENTRE).is(ModBlocks.FILLED_GHOST_TRAP.get()), "not captured yet");
            FilledGhostTrapBlockEntity filled = (FilledGhostTrapBlockEntity) helper.getBlockEntity(CENTRE, FilledGhostTrapBlockEntity.class);
            String mode = filled.contents().entity().getStringOr("AttachMode", "?");
            check(helper, mode.equals("NONE"), "the captured ghost is still %s", mode);
            check(helper, filled.seal() != null && TrapIntegrity.integrity(filled.seal(), helper.getLevel().getGameTime()) > 99.0F, "no fresh seal");
            remove(player);
        });
    }

    private static void failedCaptureAttach(GameTestHelper helper) {

        ServerPlayer player = player(helper, CENTRE.south(4));
        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE.above());
        ghost.setPresence(helper.getLevel(), 4.0F);
        helper.setBlock(CENTRE, ModBlocks.GHOST_TRAP.get());
        GhostTrapBlockEntity trap = (GhostTrapBlockEntity) helper.getBlockEntity(CENTRE, GhostTrapBlockEntity.class);
        trap.setRollOverride(() -> 0.99);
        trap.setAttachRollOverride(() -> 0.1);
        helper.succeedWhen(() -> {
            check(helper, ghost.attachment().following(), "the ghost that slipped the trap didn't attach (seeded roll 0.1)");
            check(helper, player.getUUID().equals(ghost.attachment().player()), "attached to the wrong player");
            ghost.discard();
            remove(player);
        });
    }

    /**
     * An Agitated ghost rolls once when a player first comes within 16: seeded to attach (0.1 under 0.3). A second,
     * calm ghost never rolls.
     */
    private static void agitatedNotice(GameTestHelper helper) {

        LingeringSpiritEntity agitated = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE.above());
        LingeringSpiritEntity calm = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE.east(4).above());
        agitated.agitate(2400);
        Attachment.setRollOverride(() -> 0.1);
        ServerPlayer player = player(helper, CENTRE.south(6));
        helper.succeedWhen(() -> {
            check(helper, agitated.attachment().following() && player.getUUID().equals(agitated.attachment().player()),
                    "the Agitated ghost didn't attach on noticing the player");
            check(helper, !calm.attachment().attached(), "a calm ghost attached");
            Attachment.setRollOverride(null);
            agitated.discard();
            calm.discard();
            remove(player);
        });
    }
}
