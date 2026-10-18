package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostCuriousGoal;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostShyGoal;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * GameTests for ghost movement and AI that Update 2 left to hand checks: doors and thin walls, home after reload,
 * the Curious and Shy goals. Same {@code paranormalcraft:arena} structure as {@link ModGameTests}.
 */
public final class GhostBehaviourGameTests {

    /** The sealed box the pathing tests start in: stone shell x 2..6, y 1..5, z 17..23. */
    private static final int BOX_MIN_X = 2;
    private static final int BOX_MAX_X = 6;
    /** The one opening, in the box's east wall (x = 6), two blocks high. */
    private static final BlockPos OPENING = new BlockPos(BOX_MAX_X, 1, 20);
    /** Far outside the 16-block leash, so the return-home goal drives the ghost out of the box. */
    private static final BlockPos FAR_HOME = new BlockPos(36, 1, 20);
    private static final int CURIOUS_TEST_TICKS = 600;

    private GhostBehaviourGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("ghost_paths_through_closed_door", helper -> pathsOut(helper, Blocks.OAK_DOOR));
        RegistrationHelper.testFunction("ghost_paths_through_glass_pane", helper -> pathsOut(helper, Blocks.GLASS_PANE));
        RegistrationHelper.testFunction("ghost_blocked_by_stone_wall", GhostBehaviourGameTests::blockedByStone);
        RegistrationHelper.testFunction("ghost_home_survives_reload", GhostBehaviourGameTests::homeSurvivesReload);
        RegistrationHelper.testFunction("ghost_curious_one_at_a_time", GhostBehaviourGameTests::curiousOneAtATime);
        RegistrationHelper.testFunction("ghost_shy_backs_away", GhostBehaviourGameTests::shyBacksAway);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    // TODO(26.3): makeMockServerPlayerInLevel is deprecated; switch once a replacement exists.
    @SuppressWarnings("removal")
    private static ServerPlayer playerAt(GameTestHelper helper, BlockPos relative) {

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
        player.teleportTo(at.x, at.y, at.z);
        player.setOnGround(true);
        return player;
    }

    private static void removePlayer(ServerPlayer player) {

        player.level().getServer().getPlayerList().remove(player);
    }

    private static double horizontalDistance(Entity a, Entity b) {

        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    // ---- doors, thin walls and stone ----

    /**
     * A closed stone box whose only opening is {@code exit}; the ghost starts inside with its home far outside.
     */
    private static LingeringSpiritEntity ghostInBox(GameTestHelper helper, Block exit) {

        for (int x = BOX_MIN_X; x <= BOX_MAX_X; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 17; z <= 23; z++) {
                    boolean shell = x == BOX_MIN_X || x == BOX_MAX_X || y == 5 || z == 17 || z == 23;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        if (exit instanceof DoorBlock) {
            helper.setBlock(OPENING, exit.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            helper.setBlock(OPENING.above(), exit.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        } else if (exit != Blocks.STONE) {
            helper.setBlock(OPENING, exit);
            helper.setBlock(OPENING.above(), exit);
        }
        LingeringSpiritEntity ghost = helper.spawn(ModEntities.LINGERING_SPIRIT.get(), new BlockPos(4, 1, 20));
        ghost.setHome(helper.getLevel(), helper.absolutePos(FAR_HOME));
        return ghost;
    }

    private static double relativeX(GameTestHelper helper, Entity entity) {

        return helper.relativeVec(entity.position()).x;
    }

    /**
     * The ghost leaves the box through a closed wooden door or a glass pane, and the door stays closed.
     */
    private static void pathsOut(GameTestHelper helper, Block exit) {

        LingeringSpiritEntity ghost = ghostInBox(helper, exit);
        helper.succeedWhen(() -> {
            check(helper, relativeX(helper, ghost) > BOX_MAX_X + 1.0, "ghost still in the box at x=%.2f", relativeX(helper, ghost));
            if (exit instanceof DoorBlock) {
                check(helper, !helper.getBlockState(OPENING).getValue(DoorBlock.OPEN), "the ghost opened the door");
            }
            helper.assertBlockPresent(exit, OPENING);
            ghost.discard();
        });
    }

    /**
     * Fully stone: no path home, and after 10 s the ghost is still inside.
     */
    private static void blockedByStone(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghostInBox(helper, Blocks.STONE);
        helper.runAfterDelay(200, () -> {
            Path path = ghost.getNavigation().createPath(Objects.requireNonNull(ghost.getHome()), 0);
            check(helper, path == null || !path.canReach(), "found a path home through stone");
            double x = relativeX(helper, ghost);
            check(helper, x > BOX_MIN_X && x < BOX_MAX_X, "ghost left the box (x=%.2f)", x);
            ghost.discard();
            helper.succeed();
        });
    }

    // ---- home after reload ----

    /**
     * Saves a ghost to NBT, discards it, loads it back and lets it tick: {@code Home} and {@code ColdSpot} are kept and
     * not re-picked. A roofed shelter puts the cold spot somewhere other than home, so both values are distinct.
     */
    private static void homeSurvivesReload(GameTestHelper helper) {

        for (int x = 25; x <= 27; x++) {
            for (int z = 25; z <= 27; z++) {
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
            }
        }
        ServerLevel level = helper.getLevel();
        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), new BlockPos(20, 2, 20));
        ghost.setHome(level, helper.absolutePos(new BlockPos(20, 1, 20)));
        BlockPos home = ghost.getHome();
        BlockPos coldSpot = ghost.getColdSpot();
        check(helper, coldSpot != null && !coldSpot.equals(home), "set-up: cold spot %s should differ from home %s", coldSpot, home);

        CompoundTag tag;
        Entity loaded;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
            check(helper, ghost.save(output), "ghost refused to save");
            tag = output.buildResult();
            ghost.discard();
            loaded = EntityType.loadEntityRecursive(TagValueInput.create(reporter, level.registryAccess(), tag), level, EntitySpawnReason.LOAD, entity -> entity);
        }
        check(helper, tag.contains("Home") && tag.contains("ColdSpot"), "saved tag has no Home/ColdSpot: %s", tag.keySet());
        check(helper, loaded instanceof GhostEntity, "loaded %s, not a ghost", loaded);
        GhostEntity reloaded = (GhostEntity) loaded;
        check(helper, home.equals(reloaded.getHome()), "home %s after reload, expected %s", reloaded.getHome(), home);
        check(helper, coldSpot.equals(reloaded.getColdSpot()), "cold spot %s after reload, expected %s", reloaded.getColdSpot(), coldSpot);
        level.addFreshEntity(reloaded);
        helper.runAfterDelay(5, () -> {
            check(helper, home.equals(reloaded.getHome()) && coldSpot.equals(reloaded.getColdSpot()),
                    "home/cold spot changed after ticking: %s / %s", reloaded.getHome(), reloaded.getColdSpot());
            reloaded.discard();
            helper.succeed();
        });
    }

    // ---- Curious ----

    /**
     * Two ghosts homed 3 blocks either side of one player, for 30 s: the Curious goal brings a ghost 4 to 6 blocks from them
     * for a while, and never two ghosts at once.
     */
    private static void curiousOneAtATime(GameTestHelper helper) {

        // The player stands between the ghosts' homes, so wandering (up to 12 blocks from home) keeps them in range.
        ServerPlayer player = playerAt(helper, new BlockPos(20, 1, 20));
        List<LingeringSpiritEntity> ghosts = List.of(
                helper.spawn(ModEntities.LINGERING_SPIRIT.get(), new BlockPos(17, 2, 20)),
                helper.spawn(ModEntities.LINGERING_SPIRIT.get(), new BlockPos(23, 2, 20)));
        int[] curiousTicks = {0};
        int[] hangingTicks = {0};
        helper.onEachTick(() -> {
            int curiousNow = 0;
            for (LingeringSpiritEntity ghost : ghosts) {
                boolean curious = ghost.runningGoal(GhostCuriousGoal.class).map(GhostCuriousGoal::target).orElse(null) == player;
                if (curious) {
                    curiousNow++;
                    double distance = horizontalDistance(ghost, player);
                    if (distance >= 4.0 && distance <= 6.0) {
                        hangingTicks[0]++;
                    }
                }
            }
            if (curiousNow > 1) {
                removePlayer(player);
                check(helper, false, "%d ghosts curious about the same player at tick %d", curiousNow, helper.getTick());
            }
            if (curiousNow > 0) {
                curiousTicks[0]++;
            }
        });
        helper.runAfterDelay(CURIOUS_TEST_TICKS, () -> {
            removePlayer(player);
            ghosts.forEach(ghost -> ghost.discard());
            Constants.LOG.info("Curious test: a ghost was curious for {} ticks, {} of them 4-6 blocks from the player", curiousTicks[0], hangingTicks[0]);
            check(helper, curiousTicks[0] > 0, "no ghost got curious in 30 s");
            check(helper, hangingTicks[0] >= 40, "only %d ticks spent 4-6 blocks from the player (curious for %d)", hangingTicks[0], curiousTicks[0]);
            helper.succeed();
        });
    }

    // ---- Shy ----

    /**
     * A goggle-wearing player stares at the ghost; after 2 s the Shy goal starts and the ghost ends up further from
     * them than when it started.
     */
    private static void shyBacksAway(GameTestHelper helper) {

        ServerPlayer player = playerAt(helper, new BlockPos(20, 1, 12));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
        LingeringSpiritEntity ghost = helper.spawn(ModEntities.LINGERING_SPIRIT.get(), new BlockPos(20, 2, 20));
        long startTick = helper.getTick();
        long[] shyStart = {-1};
        double[] distanceAtStart = {0};
        helper.onEachTick(() -> {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, ghost.getEyePosition());
            if (shyStart[0] < 0 && ghost.runningGoal(GhostShyGoal.class).isPresent()) {
                shyStart[0] = helper.getTick();
                distanceAtStart[0] = ghost.distanceTo(player);
            }
        });
        helper.succeedWhen(() -> {
            check(helper, shyStart[0] >= 0, "shy goal hasn't started");
            double distance = ghost.distanceTo(player);
            check(helper, distance >= distanceAtStart[0] + 2.0, "only %.1f blocks away (%.1f when shy started)", distance, distanceAtStart[0]);
            long stared = shyStart[0] - startTick;
            removePlayer(player);
            ghost.discard();
            check(helper, stared >= GhostShyGoal.STARE_TICKS, "shy after only %d ticks of staring", stared);
        });
    }
}
