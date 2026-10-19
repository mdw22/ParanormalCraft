package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlock;
import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlock;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.PresenceRules;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.capture.WandHeat;
import io.github.mdw22.paranormalcraft.debug.DebugScenes;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Presence;
import io.github.mdw22.paranormalcraft.item.TetherWandItem;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * GameTests for Milestone 2 part A: the Tether Wand, Presence floor and Weakened state, and the Ghost Trap. Mock
 * players are kept firing with {@link TetherManager#setForced} (what {@code debug tether} does), so the beam logic is
 * the real server path. Arena tests use the {@code paranormalcraft:arena} structure, ghost frozen in the middle; the
 * cabin capture builds {@code scene cabin_oak_night}, so it runs in the toolkit batch.
 */
public final class CaptureGameTests {

    private static final BlockPos CENTRE = new BlockPos(20, 2, 20);
    private static final BlockPos TRAP = CENTRE.below();
    private static final double DRAIN_TOLERANCE = 0.15;

    private CaptureGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("tether_drains_two_per_second", CaptureGameTests::drainsTwoPerSecond);
        RegistrationHelper.testFunction("tether_line_of_sight_stone", helper -> lineOfSight(helper, Blocks.STONE, true));
        RegistrationHelper.testFunction("tether_line_of_sight_glass", helper -> lineOfSight(helper, Blocks.GLASS, false));
        RegistrationHelper.testFunction("tether_breaks_at_range", CaptureGameTests::breaksAtRange);
        RegistrationHelper.testFunction("tether_breaks_without_goggles", CaptureGameTests::breaksWithoutGoggles);
        RegistrationHelper.testFunction("tether_overheats_after_8s", CaptureGameTests::overheats);
        RegistrationHelper.testFunction("presence_floor_under_melee_and_tether", CaptureGameTests::presenceFloor);
        RegistrationHelper.testFunction("weakened_and_recovers", CaptureGameTests::weakenedAndRecovers);
        RegistrationHelper.testFunction("trap_capture_with_tether", CaptureGameTests::captureWithTether);
        RegistrationHelper.testFunction("trap_capture_untethered_roll_wins", helper -> captureUntethered(helper, 0.3, true));
        RegistrationHelper.testFunction("trap_capture_untethered_roll_fails", helper -> captureUntethered(helper, 0.7, false));
        RegistrationHelper.testFunction("trap_ignores_unweakened_ghost", CaptureGameTests::ignoresUnweakened);
        RegistrationHelper.testFunction("trap_broken_mid_capture_frees_ghost", CaptureGameTests::brokenMidCapture);
        RegistrationHelper.testFunction("filled_trap_round_trip", CaptureGameTests::filledTrapRoundTrip);
        RegistrationHelper.testFunction("two_tethers_slow_ghost", CaptureGameTests::twoTethersSlow);
        RegistrationHelper.testFunction("crossing_beams_backfire", CaptureGameTests::crossingBeamsBackfire);
        RegistrationHelper.testFunction("coop_capture_no_backfire", CaptureGameTests::coopCaptureNoBackfire);
        RegistrationHelper.testFunction("trap_capture_in_cabin", CaptureGameTests::captureInCabin);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    // ---- set-up ----

    private static LingeringSpiritEntity frozenGhost(GameTestHelper helper, float presence) {

        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE);
        ghost.setPresence(helper.getLevel(), presence);
        return ghost;
    }

    /**
     * A survival mock player at {@code at} (absolute), holding a cold wand, with goggles if asked, looking at
     * {@code target}.
     */
    // TODO(26.3): makeMockServerPlayerInLevel is deprecated; switch once a replacement exists.
    @SuppressWarnings("removal")
    private static ServerPlayer hunter(GameTestHelper helper, Vec3 at, boolean goggles, Vec3 target) {

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.teleportTo(at.x, at.y, at.z);
        player.setOnGround(true);
        if (goggles) {
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TETHER_WAND.get()));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
        return player;
    }

    private static ServerPlayer hunter(GameTestHelper helper, BlockPos relative, Entity target) {

        return hunter(helper, helper.absoluteVec(Vec3.atBottomCenterOf(relative)), true, target.getBoundingBox().getCenter());
    }

    private static boolean latchedOn(ServerPlayer player, GhostEntity ghost) {

        TetherManager.Tether tether = TetherManager.tetherOf(player);
        return tether != null && ghost.getUUID().equals(tether.ghost());
    }

    private static void finish(GameTestHelper helper, ServerPlayer... players) {

        for (ServerPlayer player : players) {
            TetherManager.setForced(player, false);
            player.level().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static GhostTrapBlockEntity trap(GameTestHelper helper, BlockPos relative, double roll) {

        helper.setBlock(relative, ModBlocks.GHOST_TRAP.get());
        if (!(helper.getBlockEntity(relative, GhostTrapBlockEntity.class) instanceof GhostTrapBlockEntity trap)) {
            throw helper.assertionException(Component.literal("no trap block entity"));
        }
        trap.setRollOverride(() -> roll);
        return trap;
    }

    // ---- the tether ----

    /**
     * A latched tether drains 2 Presence a second: 20 ticks take 2.0 off a full ghost.
     */
    private static void drainsTwoPerSecond(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, Presence.MAX);
        ServerPlayer player = hunter(helper, CENTRE.offset(0, -1, -6), ghost);
        TetherManager.setForced(player, true);
        float[] start = new float[1];
        helper.runAfterDelay(5, () -> {
            check(helper, latchedOn(player, ghost), "the tether didn't latch from 6 blocks");
            start[0] = ghost.getPresence();
        });
        helper.runAfterDelay(25, () -> {
            float drained = start[0] - ghost.getPresence();
            check(helper, Math.abs(drained - 2.0F) <= DRAIN_TOLERANCE, "20 ticks drained %.2f, expected 2.0", drained);
            finish(helper, player);
        });
    }

    /**
     * A wall put between player and ghost: stone breaks the tether after 20 ticks (not before); glass never does.
     */
    private static void lineOfSight(GameTestHelper helper, Block wall, boolean breaks) {

        LingeringSpiritEntity ghost = frozenGhost(helper, Presence.MAX);
        ServerPlayer player = hunter(helper, CENTRE.offset(0, -1, -6), ghost);
        TetherManager.setForced(player, true);
        helper.runAfterDelay(5, () -> {
            check(helper, latchedOn(player, ghost), "the tether didn't latch");
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 2; y++) {
                    helper.setBlock(CENTRE.offset(x, y, -3), wall);
                }
            }
        });
        helper.runAfterDelay(5 + TetherManager.LOS_GRACE_TICKS - 5, () ->
                check(helper, latchedOn(player, ghost), "the tether broke early behind %s", wall));
        helper.runAfterDelay(5 + TetherManager.LOS_GRACE_TICKS + 15, () -> {
            check(helper, latchedOn(player, ghost) != breaks, "behind %s the tether %s", wall, breaks ? "held" : "broke");
            finish(helper, player);
        });
    }

    /**
     * At 13.5 blocks the tether holds; past 14 it breaks.
     */
    private static void breaksAtRange(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, Presence.MAX);
        ServerPlayer player = hunter(helper, CENTRE.offset(0, -1, -10), ghost);
        TetherManager.setForced(player, true);
        Vec3 ghostAt = ghost.position();
        helper.runAfterDelay(5, () -> {
            check(helper, latchedOn(player, ghost), "the tether didn't latch from 10 blocks");
            player.teleportTo(ghostAt.x, ghostAt.y - 1.0, ghostAt.z - 13.4);
        });
        helper.runAfterDelay(10, () -> {
            check(helper, latchedOn(player, ghost), "broke at %.2f blocks", player.distanceTo(ghost));
            player.teleportTo(ghostAt.x, ghostAt.y - 1.0, ghostAt.z - 14.5);
        });
        helper.runAfterDelay(13, () -> {
            check(helper, !latchedOn(player, ghost), "still latched at %.2f blocks", player.distanceTo(ghost));
            finish(helper, player);
        });
    }

    /**
     * Taking the goggles off breaks the tether on a ghost the player can no longer see.
     */
    private static void breaksWithoutGoggles(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, Presence.MAX);
        ServerPlayer player = hunter(helper, CENTRE.offset(0, -1, -6), ghost);
        TetherManager.setForced(player, true);
        helper.runAfterDelay(5, () -> {
            check(helper, latchedOn(player, ghost), "the tether didn't latch");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        });
        helper.runAfterDelay(8, () -> {
            check(helper, !latchedOn(player, ghost), "still latched without goggles");
            finish(helper, player);
        });
    }

    /**
     * 8 seconds of continuous fire overheats the wand (lockout set, beam ended); not a tick before.
     */
    private static void overheats(GameTestHelper helper) {

        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(5, 1, 5)));
        ServerPlayer player = hunter(helper, at, true, at.add(10, 1.6, 0));
        ItemStack wand = player.getMainHandItem();
        TetherManager.setForced(player, true);
        long[] firstHeat = {-1};
        long[] overheatedAt = {-1};
        helper.onEachTick(() -> {
            WandHeat heat = TetherWandItem.heat(wand);
            long now = helper.getTick();
            if (firstHeat[0] < 0 && heat.heat() > 0) {
                firstHeat[0] = now - heat.heat() + 1;
            }
            if (overheatedAt[0] < 0 && heat.overheated()) {
                overheatedAt[0] = now;
            }
        });
        helper.succeedWhen(() -> {
            check(helper, overheatedAt[0] >= 0, "not overheated yet (heat %d)", TetherWandItem.heat(wand).heat());
            long firedTicks = overheatedAt[0] - firstHeat[0] + 1;
            check(helper, Math.abs(firedTicks - WandHeat.MAX_HEAT) <= 2, "overheated after %d ticks of fire, expected %d", firedTicks, WandHeat.MAX_HEAT);
            check(helper, TetherWandItem.heat(wand).lockout() > 0, "no lockout");
            check(helper, TetherManager.tetherOf(player) == null, "the beam is still on while overheated");
            TetherManager.setForced(player, false);
            player.level().getServer().getPlayerList().remove(player);
        });
    }

    // ---- Presence and Weakened ----

    /**
     * A 100-damage punch leaves 1 Presence and a live, Weakened ghost; a tether on a ghost at 2 stops at 1.
     */
    private static void presenceFloor(GameTestHelper helper) {

        LingeringSpiritEntity punched = frozenGhost(helper, Presence.MAX);
        ServerPlayer puncher = hunter(helper, CENTRE.offset(4, -1, 0), punched);
        punched.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(puncher), 100.0F);
        check(helper, !punched.isRemoved() && punched.getPresence() == PresenceRules.FLOOR,
                "after a huge punch: removed=%s, presence %.2f", punched.isRemoved(), punched.getPresence());
        check(helper, punched.isWeakened(), "a ghost at the floor isn't Weakened");
        punched.discard();

        LingeringSpiritEntity tethered = frozenGhost(helper, 2.0F);
        ServerPlayer holder = hunter(helper, CENTRE.offset(0, -1, -6), tethered);
        TetherManager.setForced(holder, true);
        helper.runAfterDelay(40, () -> {
            check(helper, latchedOn(holder, tethered), "the tether let go");
            check(helper, !tethered.isRemoved() && tethered.getPresence() == PresenceRules.FLOOR,
                    "after 2 s of tether from 2: removed=%s, presence %.2f", tethered.isRemoved(), tethered.getPresence());
            finish(helper, puncher, holder);
        });
    }

    /**
     * A punch from 6 to 5 sets the synced Weakened flag (what clients flicker on); 15 s later, untethered, the ghost
     * recovers to 10 and the flag clears.
     */
    private static void weakenedAndRecovers(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, 6.0F);
        ServerPlayer puncher = hunter(helper, CENTRE.offset(4, -1, 0), ghost);
        check(helper, !ghost.isWeakened(), "Weakened at 6");
        ghost.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(puncher), 2.0F);
        check(helper, ghost.getPresence() == 5.0F, "a 2-damage punch left %.2f, expected 5 (half of 2 drained)", ghost.getPresence());
        check(helper, ghost.isWeakened(), "not Weakened at 5");
        check(helper, ghost.recoveryTicksLeft() > PresenceRules.RECOVERY_TICKS - 5, "recovery timer %d", ghost.recoveryTicksLeft());
        helper.runAfterDelay(PresenceRules.RECOVERY_TICKS - 20, () ->
                check(helper, ghost.isWeakened() && ghost.getPresence() == 5.0F, "recovered early (presence %.2f)", ghost.getPresence()));
        helper.runAfterDelay(PresenceRules.RECOVERY_TICKS + 10, () -> {
            check(helper, !ghost.isWeakened(), "still Weakened after 15 s");
            check(helper, ghost.getPresence() >= PresenceRules.RECOVERED_PRESENCE, "recovered to %.2f, expected %.0f",
                    ghost.getPresence(), PresenceRules.RECOVERED_PRESENCE);
            finish(helper, puncher);
        });
    }

    // ---- the trap ----

    /**
     * Tethered from 6, a ghost at 6 Presence drains into Weakened over the trap and is captured, even on a roll that
     * would fail untethered.
     */
    private static void captureWithTether(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, 6.0F);
        ServerPlayer player = hunter(helper, CENTRE.offset(0, -1, -6), ghost);
        TetherManager.setForced(player, true);
        helper.runAfterDelay(3, () -> {
            check(helper, latchedOn(player, ghost), "the tether didn't latch");
            trap(helper, TRAP, 0.99);
        });
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.FILLED_GHOST_TRAP.get(), TRAP);
            check(helper, ghost.isRemoved(), "the ghost is still out");
            TetherManager.setForced(player, false);
            player.level().getServer().getPlayerList().remove(player);
        });
    }

    /**
     * Untethered, the capture is a coin flip: a low roll fills the trap; a high one lets the ghost break free at 8,
     * no longer Weakened, and leaves the trap armed and closed.
     */
    private static void captureUntethered(GameTestHelper helper, double roll, boolean wins) {

        LingeringSpiritEntity ghost = frozenGhost(helper, 5.0F);
        trap(helper, TRAP, roll);
        if (wins) {
            helper.succeedWhen(() -> {
                helper.assertBlockPresent(ModBlocks.FILLED_GHOST_TRAP.get(), TRAP);
                check(helper, ghost.isRemoved(), "the ghost is still out");
            });
            return;
        }
        helper.runAfterDelay(10, () -> helper.assertBlockProperty(TRAP, GhostTrapBlock.OPEN, true));
        helper.runAfterDelay(40, () -> {
            helper.assertBlockPresent(ModBlocks.GHOST_TRAP.get(), TRAP);
            helper.assertBlockProperty(TRAP, GhostTrapBlock.OPEN, false);
            check(helper, !ghost.isRemoved(), "the ghost was taken on a losing roll");
            check(helper, ghost.getPresence() == PresenceRules.BREAK_FREE_PRESENCE && !ghost.isWeakened(),
                    "after breaking free: presence %.2f, weakened %s", ghost.getPresence(), ghost.isWeakened());
            check(helper, ghost.capturingTrap() == null, "still held by the trap");
            helper.succeed();
        });
    }

    /**
     * A ghost at full Presence floats over an armed trap and nothing happens.
     */
    private static void ignoresUnweakened(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, Presence.MAX);
        trap(helper, TRAP, 0.0);
        helper.runAfterDelay(40, () -> {
            helper.assertBlockPresent(ModBlocks.GHOST_TRAP.get(), TRAP);
            helper.assertBlockProperty(TRAP, GhostTrapBlock.OPEN, false);
            check(helper, !ghost.isRemoved() && ghost.capturingTrap() == null, "the trap went for a full-Presence ghost");
            helper.succeed();
        });
    }

    /**
     * Breaking the trap halfway through a capture lets the ghost go, with its AI back.
     */
    private static void brokenMidCapture(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, 5.0F);
        ghost.setNoAi(false);
        trap(helper, TRAP, 0.0);
        helper.runAfterDelay(15, () -> {
            check(helper, ghost.capturingTrap() != null, "the capture didn't start");
            helper.setBlock(TRAP, Blocks.AIR);
        });
        helper.runAfterDelay(17, () -> {
            check(helper, !ghost.isRemoved() && ghost.capturingTrap() == null && !ghost.isNoAi(),
                    "after breaking the trap: removed %s, held by %s, noAi %s", ghost.isRemoved(), ghost.capturingTrap(), ghost.isNoAi());
            helper.succeed();
        });
    }

    /**
     * Capture, pick up as an item, place the item as a block elsewhere, release: the same ghost (UUID, name, home,
     * cold spot) comes out at full Presence, and that trap is armed and empty again.
     */
    private static void filledTrapRoundTrip(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, 5.0F);
        BlockPos home = helper.absolutePos(new BlockPos(30, 2, 30));
        BlockPos coldSpot = helper.absolutePos(new BlockPos(30, 1, 31));
        ghost.setHome(home, coldSpot);
        ghost.setGhostName("Test Spirit");
        UUID id = ghost.getUUID();
        trap(helper, TRAP, 0.0);
        BlockPos elsewhere = new BlockPos(10, 1, 10);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.FILLED_GHOST_TRAP.get(), TRAP);
            FilledGhostTrapBlockEntity filled = helper.getBlockEntity(TRAP, FilledGhostTrapBlockEntity.class);
            CapturedGhost contents = filled.contents();
            check(helper, contents != null && contents.name().equals("Test Spirit"), "trap holds %s", contents);
            ItemStack item = FilledGhostTrapBlock.itemFor(filled);
            check(helper, item.has(ModDataComponents.CAPTURED_GHOST.get()), "the item lost the ghost");
            helper.setBlock(TRAP, Blocks.AIR);

            helper.setBlock(elsewhere, ModBlocks.FILLED_GHOST_TRAP.get());
            FilledGhostTrapBlockEntity placed = helper.getBlockEntity(elsewhere, FilledGhostTrapBlockEntity.class);
            placed.applyComponentsFromItemStack(item);
            GhostEntity released = FilledGhostTrapBlock.release(helper.getLevel(), helper.absolutePos(elsewhere), placed);
            check(helper, released != null, "nothing came out");
            check(helper, released.getUUID().equals(id), "a different ghost came out (%s, expected %s)", released.getUUID(), id);
            check(helper, released.ghostName().equals("Test Spirit"), "name %s", released.ghostName());
            check(helper, home.equals(released.getHome()), "home %s, expected %s", released.getHome(), home);
            check(helper, coldSpot.equals(released.getColdSpot()), "cold spot %s, expected %s", released.getColdSpot(), coldSpot);
            check(helper, released.getPresence() == Presence.MAX && !released.isWeakened(), "released at %.2f", released.getPresence());
            check(helper, !released.isNoAi(), "released ghost has no AI");
            helper.assertBlockPresent(ModBlocks.GHOST_TRAP.get(), elsewhere);
            released.discard();
        });
    }

    /**
     * Two tethers on one ghost: each drains (4 a second together), and its speed factor halves.
     */
    private static void twoTethersSlow(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, Presence.MAX);
        ServerPlayer first = hunter(helper, CENTRE.offset(0, -1, -6), ghost);
        ServerPlayer second = hunter(helper, CENTRE.offset(-6, -1, 0), ghost);
        TetherManager.setForced(first, true);
        TetherManager.setForced(second, true);
        float[] start = new float[1];
        helper.runAfterDelay(5, () -> {
            check(helper, latchedOn(first, ghost) && latchedOn(second, ghost), "both tethers didn't latch");
            check(helper, ghost.tetherCount() == 2, "tether count %d", ghost.tetherCount());
            check(helper, ghost.speedFactor() == 0.5, "speed factor %.2f with two tethers", ghost.speedFactor());
            start[0] = ghost.getPresence();
        });
        helper.runAfterDelay(25, () -> {
            float drained = start[0] - ghost.getPresence();
            check(helper, Math.abs(drained - 4.0F) <= 2 * DRAIN_TOLERANCE, "two tethers drained %.2f in 20 ticks, expected 4.0", drained);
            finish(helper, first, second);
        });
    }

    /**
     * Two free beams crossing at right angles: both wands overheat, both beams end, both players take 2 hearts.
     */
    private static void crossingBeamsBackfire(GameTestHelper helper) {

        Vec3 meet = helper.absoluteVec(new Vec3(20.5, 2.4, 20.5));
        Vec3 westAt = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(14, 1, 20)));
        Vec3 northAt = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(20, 1, 14)));
        ServerPlayer west = hunter(helper, westAt, true, new Vec3(meet.x + 6, westAt.y + 1.62, westAt.z));
        ServerPlayer north = hunter(helper, northAt, true, new Vec3(northAt.x, northAt.y + 1.62, meet.z + 6));
        float healthBefore = west.getHealth();
        TetherManager.setForced(west, true);
        TetherManager.setForced(north, true);
        helper.succeedWhen(() -> {
            for (ServerPlayer player : List.of(west, north)) {
                check(helper, TetherWandItem.heat(player.getMainHandItem()).overheated(), "a wand didn't overheat");
                check(helper, TetherManager.tetherOf(player) == null, "a beam is still on");
                check(helper, player.getHealth() <= healthBefore - TetherManager.BACKFIRE_DAMAGE + 0.01F, "health %.1f after the backfire", player.getHealth());
            }
            for (ServerPlayer player : List.of(west, north)) {
                TetherManager.setForced(player, false);
                player.level().getServer().getPlayerList().remove(player);
            }
        });
    }

    /**
     * Two tethers hold a ghost into a trap. When it is captured both beams end, and stay off: two free beams aimed at
     * the same spot would otherwise cross and backfire on both players.
     */
    private static void coopCaptureNoBackfire(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper, 6.0F);
        ServerPlayer first = hunter(helper, CENTRE.offset(0, -1, -6), ghost);
        ServerPlayer second = hunter(helper, CENTRE.offset(-6, -1, 0), ghost);
        float health = first.getHealth();
        TetherManager.setForced(first, true);
        TetherManager.setForced(second, true);
        helper.runAfterDelay(2, () -> trap(helper, TRAP, 0.99));
        boolean[] filledSeen = {false};
        helper.onEachTick(() -> filledSeen[0] |= helper.getBlockState(TRAP).is(ModBlocks.FILLED_GHOST_TRAP.get()));
        helper.runAfterDelay(70, () -> {
            check(helper, filledSeen[0] && ghost.isRemoved(), "the co-op capture didn't happen");
            for (ServerPlayer player : List.of(first, second)) {
                check(helper, TetherManager.tetherOf(player) == null, "a beam is still on after the capture");
                check(helper, !TetherWandItem.heat(player.getMainHandItem()).overheated(), "a wand overheated (backfire?)");
                check(helper, player.getHealth() == health, "a player took damage (health %.1f)", player.getHealth());
            }
            finish(helper, first, second);
        });
    }

    /**
     * In {@code scene cabin_oak_night}: the cabin ghost, Weakened and held over a trap on its home floor in the back
     * room, is captured with a tether from inside the cabin.
     */
    private static void captureInCabin(GameTestHelper helper) {

        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();
        DebugScenes.Placement placement = DebugScenes.build(server, "cabin_oak_night", null).orElse(null);
        check(helper, placement != null && placement.ghosts().size() == 1, "the cabin scene didn't build one ghost");
        LingeringSpiritEntity ghost = placement.ghosts().getFirst();
        BlockPos home = ghost.getHome();
        check(helper, home != null && level.getBlockState(home).isAir(), "ghost home %s isn't open floor", home);
        BlockPos standAt = null;
        Vec3 over = Vec3.atBottomCenterOf(home).add(0, 0.6, 0);
        for (int distance = 4; distance >= 2 && standAt == null; distance--) {
            for (BlockPos candidate : List.of(home.west(distance), home.east(distance), home.north(distance), home.south(distance))) {
                if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()
                        && level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)
                        && TetherManager.hasLineOfSight(level, Vec3.atBottomCenterOf(candidate).add(0, 1.62, 0), over.add(0, 0.4, 0))) {
                    standAt = candidate;
                    break;
                }
            }
        }
        check(helper, standAt != null, "nowhere inside to stand near home %s", home.toShortString());
        // Nobody real is near the scene, so keep its chunk ticking (block entities only tick in ticking chunks).
        int chunkX = home.getX() >> 4;
        int chunkZ = home.getZ() >> 4;
        level.setChunkForced(chunkX, chunkZ, true);
        ghost.setNoAi(true);
        ghost.snapTo(over.x, over.y, over.z, 0.0F, 0.0F);
        ServerPlayer player = hunter(helper, Vec3.atBottomCenterOf(standAt), true, over.add(0, 0.4, 0));
        TetherManager.setForced(player, true);
        BlockPos trapPos = home;
        helper.runAfterDelay(3, () -> {
            TetherManager.Tether tether = TetherManager.tetherOf(player);
            check(helper, latchedOn(player, ghost), "the tether didn't latch inside the cabin: player %s eye %s look %s, ghost %s (removed %s), tether %s end %s",
                    player.position(), player.getEyePosition(), player.getViewVector(1.0F), ghost.position(), ghost.isRemoved(),
                    tether == null ? "none" : tether.ghost(), tether == null ? "-" : tether.end());
            ghost.setPresence(level, PresenceRules.WEAKENED_AT);
            level.setBlock(trapPos, ModBlocks.GHOST_TRAP.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(trapPos) instanceof GhostTrapBlockEntity trap) {
                trap.setRollOverride(() -> 0.99);
            }
        });
        helper.succeedWhen(() -> {
            check(helper, level.getBlockState(trapPos).is(ModBlocks.FILLED_GHOST_TRAP.get()), "trap at %s is %s (ghost at %s, presence %.2f, weakened %s, tethers %d)",
                    trapPos.toShortString(), level.getBlockState(trapPos), ghost.position(), ghost.getPresence(), ghost.isWeakened(), ghost.tetherCount());
            check(helper, ghost.isRemoved(), "the ghost is still out");
            TetherManager.setForced(player, false);
            server.getPlayerList().remove(player);
            level.setBlock(trapPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setChunkForced(chunkX, chunkZ, false);
        });
    }
}
