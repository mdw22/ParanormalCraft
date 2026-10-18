package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.config.ServerConfig;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.haunting.HauntingAction;
import io.github.mdw22.paranormalcraft.haunting.HauntingController;
import io.github.mdw22.paranormalcraft.haunting.HauntingSchedule;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import io.github.mdw22.paranormalcraft.tool.ToolTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Server GameTests for haunting. Each stages targets in the {@code paranormalcraft:arena} next to a frozen ghost and
 * forces actions with {@link HauntingController#force} (which ignores cooldowns but still obeys the griefing rule).
 * Tests that change the global config or need no players around run in test environments of their own.
 */
public final class HauntingGameTests {

    private static final BlockPos GHOST = new BlockPos(20, 2, 20);

    private HauntingGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("haunt_knock_off_decoration", HauntingGameTests::knockOffDecoration);
        RegistrationHelper.testFunction("haunt_snuff_light", HauntingGameTests::snuffLight);
        RegistrationHelper.testFunction("haunt_never_snuffs_soul_or_redstone_lights", HauntingGameTests::neverSnuffsSoulOrRedstone);
        RegistrationHelper.testFunction("haunt_flip_lever", HauntingGameTests::flipLever);
        RegistrationHelper.testFunction("haunt_toggle_openings", HauntingGameTests::toggleOpenings);
        RegistrationHelper.testFunction("haunt_never_toggles_iron_doors", HauntingGameTests::neverTogglesIronDoors);
        RegistrationHelper.testFunction("haunt_phantom_chest_leaves_inventory", HauntingGameTests::phantomChestLeavesInventory);
        RegistrationHelper.testFunction("haunt_toss_items", HauntingGameTests::tossItems);
        RegistrationHelper.testFunction("haunt_footsteps", HauntingGameTests::footsteps);
        RegistrationHelper.testFunction("haunt_knocking", HauntingGameTests::knocking);
        RegistrationHelper.testFunction("haunt_griefing_blocked_by_mob_griefing", HauntingGameTests::griefingBlockedByGameRule);
        RegistrationHelper.testFunction("haunt_griefing_blocked_by_config", HauntingGameTests::griefingBlockedByConfig);
        RegistrationHelper.testFunction("haunt_idle_without_players", HauntingGameTests::idleWithoutPlayers);
        RegistrationHelper.testFunction("haunt_interval_and_cooldowns", HauntingGameTests::intervalAndCooldowns);
        RegistrationHelper.testFunction("haunt_spikes_emf_holders", HauntingGameTests::spikesEmfHolders);
    }

    // ---- helpers ----

    private static LingeringSpiritEntity ghost(GameTestHelper helper) {

        return helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), GHOST);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    private static Optional<HauntingController.Record> force(GameTestHelper helper, LingeringSpiritEntity ghost, HauntingAction action) {

        return ghost.haunting().force(helper.getLevel(), action);
    }

    private static void wall(GameTestHelper helper, int x, int z1, int z2) {

        for (int y = 1; y <= 3; y++) {
            for (int z = z1; z <= z2; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    private static void door(GameTestHelper helper, BlockPos lower, Block door) {

        BlockState state = door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(lower, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static boolean open(GameTestHelper helper, BlockPos pos) {

        return helper.getBlockState(pos).getValue(BlockStateProperties.OPEN);
    }

    private static long itemsNear(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item item) {

        AABB box = new AABB(helper.absolutePos(pos)).inflate(3);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, entity -> entity.getItem().is(item)).size();
    }

    // TODO(26.3): makeMockServerPlayerInLevel is deprecated; switch once a replacement exists.
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

    /**
     * Runs {@code body} with a modified server config, restoring the original even if it throws.
     */
    private static void withConfig(ServerConfig config, Runnable body) {

        ServerConfig original = ModConfigs.server();
        ModConfigs.setServerForTesting(config);
        try {
            body.run();
        } finally {
            ModConfigs.setServerForTesting(original);
        }
    }

    // ---- each action on a staged target ----

    private static void knockOffDecoration(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        ServerLevel level = helper.getLevel();
        wall(helper, 24, 18, 22);
        ItemFrame frame = new ItemFrame(level, helper.absolutePos(new BlockPos(23, 2, 19)), helper.getAbsoluteDirection(Direction.WEST));
        frame.setItem(new ItemStack(Items.FLOWER_POT), false);
        level.addFreshEntity(frame);
        Painting painting = Painting.create(level, helper.absolutePos(new BlockPos(23, 2, 21)), helper.getAbsoluteDirection(Direction.WEST)).orElseThrow();
        level.addFreshEntity(painting);

        check(helper, force(helper, ghost, HauntingAction.KNOCK_OFF_DECORATION).isPresent(), "first decoration wasn't knocked off");
        check(helper, force(helper, ghost, HauntingAction.KNOCK_OFF_DECORATION).isPresent(), "second decoration wasn't knocked off");
        check(helper, frame.isRemoved() && painting.isRemoved(), "frame removed=%s painting removed=%s", frame.isRemoved(), painting.isRemoved());
        check(helper, itemsNear(helper, new BlockPos(23, 2, 19), Items.ITEM_FRAME) == 1, "item frame didn't drop");
        check(helper, itemsNear(helper, new BlockPos(23, 2, 19), Items.FLOWER_POT) == 1, "item frame's contents didn't drop");
        check(helper, itemsNear(helper, new BlockPos(23, 2, 21), Items.PAINTING) == 1, "painting didn't drop");
        ghost.discard();
        helper.succeed();
    }

    private static void snuffLight(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        BlockPos torch = new BlockPos(22, 1, 20);
        helper.setBlock(torch, Blocks.TORCH);
        check(helper, force(helper, ghost, HauntingAction.SNUFF_LIGHT).isPresent(), "torch not snuffed");
        check(helper, helper.getBlockState(torch).isAir() && itemsNear(helper, torch, Items.TORCH) == 1, "torch didn't pop off as an item");

        BlockPos candle = new BlockPos(18, 1, 20);
        helper.setBlock(candle, Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        check(helper, force(helper, ghost, HauntingAction.SNUFF_LIGHT).isPresent(), "candle not snuffed");
        check(helper, !helper.getBlockState(candle).getValue(BlockStateProperties.LIT) && helper.getBlockState(candle).is(Blocks.CANDLE), "candle still lit");

        BlockPos campfire = new BlockPos(20, 1, 23);
        helper.setBlock(campfire, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        check(helper, force(helper, ghost, HauntingAction.SNUFF_LIGHT).isPresent(), "campfire not snuffed");
        check(helper, !helper.getBlockState(campfire).getValue(BlockStateProperties.LIT), "campfire still lit");

        check(helper, force(helper, ghost, HauntingAction.SNUFF_LIGHT).isEmpty(), "found a light to snuff when none were left lit");
        ghost.discard();
        helper.succeed();
    }

    private static void neverSnuffsSoulOrRedstone(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        helper.setBlock(new BlockPos(22, 1, 20), Blocks.SOUL_TORCH);
        helper.setBlock(new BlockPos(18, 1, 20), Blocks.REDSTONE_TORCH);
        helper.setBlock(new BlockPos(20, 1, 22), Blocks.SOUL_LANTERN);
        helper.setBlock(new BlockPos(20, 1, 18), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        check(helper, force(helper, ghost, HauntingAction.SNUFF_LIGHT).isEmpty(), "a soul or redstone light was chosen");
        helper.assertBlockPresent(Blocks.SOUL_TORCH, new BlockPos(22, 1, 20));
        helper.assertBlockPresent(Blocks.REDSTONE_TORCH, new BlockPos(18, 1, 20));
        helper.assertBlockPresent(Blocks.SOUL_LANTERN, new BlockPos(20, 1, 22));
        check(helper, helper.getBlockState(new BlockPos(20, 1, 18)).getValue(BlockStateProperties.LIT), "soul campfire was put out");
        ghost.discard();
        helper.succeed();
    }

    private static void flipLever(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        BlockPos lever = new BlockPos(22, 1, 20);
        helper.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        check(helper, force(helper, ghost, HauntingAction.FLIP_LEVER).isPresent(), "lever not flipped");
        check(helper, helper.getBlockState(lever).getValue(LeverBlock.POWERED), "lever isn't on");
        ghost.discard();
        helper.succeed();
    }

    private static void toggleOpenings(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        BlockPos door = new BlockPos(22, 1, 20);
        door(helper, door, Blocks.SPRUCE_DOOR);
        check(helper, force(helper, ghost, HauntingAction.TOGGLE_OPENING).isPresent(), "door not toggled");
        check(helper, open(helper, door) && open(helper, door.above()), "door (both halves) isn't open");
        helper.setBlock(door, Blocks.AIR);
        helper.setBlock(door.above(), Blocks.AIR);

        BlockPos trapdoor = new BlockPos(18, 1, 20);
        helper.setBlock(trapdoor, Blocks.CHERRY_TRAPDOOR);
        check(helper, force(helper, ghost, HauntingAction.TOGGLE_OPENING).isPresent() && open(helper, trapdoor), "trapdoor not opened");
        helper.setBlock(trapdoor, Blocks.AIR);

        BlockPos gate = new BlockPos(20, 1, 22);
        helper.setBlock(gate, Blocks.OAK_FENCE_GATE);
        check(helper, force(helper, ghost, HauntingAction.TOGGLE_OPENING).isPresent() && open(helper, gate), "fence gate not opened");
        check(helper, force(helper, ghost, HauntingAction.TOGGLE_OPENING).isPresent() && !open(helper, gate), "fence gate not closed again");
        ghost.discard();
        helper.succeed();
    }

    private static void neverTogglesIronDoors(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        door(helper, new BlockPos(22, 1, 20), Blocks.IRON_DOOR);
        helper.setBlock(new BlockPos(18, 1, 20), Blocks.IRON_TRAPDOOR);
        door(helper, new BlockPos(20, 1, 22), Blocks.COPPER_DOOR.weathering().unaffected());
        check(helper, force(helper, ghost, HauntingAction.TOGGLE_OPENING).isEmpty(), "an iron or copper opening was chosen");
        check(helper, !open(helper, new BlockPos(22, 1, 20)) && !open(helper, new BlockPos(18, 1, 20)) && !open(helper, new BlockPos(20, 1, 22)),
                "an iron or copper opening moved");
        ghost.discard();
        helper.succeed();
    }

    private static void phantomChestLeavesInventory(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        BlockPos chestPos = new BlockPos(22, 1, 20);
        helper.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        chest.setItem(4, new ItemStack(Items.DIAMOND, 3));
        chest.setItem(13, new ItemStack(Items.PAPER, 1));
        BlockPos barrelPos = new BlockPos(18, 1, 20);

        check(helper, force(helper, ghost, HauntingAction.PHANTOM_CHEST).isPresent(), "chest didn't rattle");
        helper.setBlock(barrelPos, Blocks.BARREL);
        Optional<HauntingController.Record> barrel = Optional.empty();
        // The chest is still a target too; keep forcing until the barrel is the one picked.
        for (int attempt = 0; attempt < 40 && barrel.isEmpty(); attempt++) {
            Optional<HauntingController.Record> record = force(helper, ghost, HauntingAction.PHANTOM_CHEST);
            if (record.isPresent() && record.get().detail().contains("barrel")) {
                barrel = record;
            }
        }
        check(helper, barrel.isPresent(), "barrel never rattled");
        check(helper, helper.getBlockState(barrelPos).getValue(BarrelBlock.OPEN), "barrel isn't open right after");
        helper.runAfterDelay(40, () -> {
            check(helper, !helper.getBlockState(barrelPos).getValue(BarrelBlock.OPEN), "barrel didn't close again");
            ChestBlockEntity after = (ChestBlockEntity) helper.getBlockEntity(chestPos, ChestBlockEntity.class);
            check(helper, after.getItem(4).is(Items.DIAMOND) && after.getItem(4).getCount() == 3 && after.getItem(13).is(Items.PAPER)
                    && after.getItem(13).getCount() == 1, "chest inventory changed");
            int filled = 0;
            for (int slot = 0; slot < after.getContainerSize(); slot++) {
                filled += after.getItem(slot).isEmpty() ? 0 : 1;
            }
            check(helper, filled == 2, "chest has %s filled slots, expected 2", filled);
            ghost.discard();
            helper.succeed();
        });
    }

    private static void tossItems(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        ItemEntity item = helper.spawnItem(Items.STRING, new BlockPos(22, 1, 20));
        item.setDeltaMovement(Vec3.ZERO);
        check(helper, force(helper, ghost, HauntingAction.TOSS_ITEMS).isPresent(), "nothing tossed");
        check(helper, item.getDeltaMovement().y > 0.1, "item wasn't pushed (motion %s)", item.getDeltaMovement());
        ghost.discard();
        helper.succeed();
    }

    private static void footsteps(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        check(helper, force(helper, ghost, HauntingAction.FOOTSTEPS).isEmpty(), "footsteps with nobody to follow");
        ServerPlayer player = playerAt(helper, new BlockPos(23, 1, 20));
        Optional<HauntingController.Record> record = force(helper, ghost, HauntingAction.FOOTSTEPS);
        removePlayer(player);
        check(helper, record.isPresent() && record.get().detail().contains("step"), "no footsteps behind a nearby player");
        ghost.discard();
        helper.succeed();
    }

    private static void knocking(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        check(helper, force(helper, ghost, HauntingAction.KNOCKING).isEmpty(), "knocked on an open floor");
        wall(helper, 24, 19, 21);
        Optional<HauntingController.Record> record = force(helper, ghost, HauntingAction.KNOCKING);
        check(helper, record.isPresent() && record.get().detail().contains("stone"), "didn't knock on the wall");
        ghost.discard();
        helper.succeed();
    }

    // ---- griefing gate ----

    /**
     * Stages a torch, a lever, an item frame and a door; checks only the door moves.
     */
    private static void griefingIsBlocked(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(22, 1, 20), Blocks.TORCH);
        helper.setBlock(new BlockPos(18, 1, 20), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        wall(helper, 24, 18, 18);
        ItemFrame frame = new ItemFrame(level, helper.absolutePos(new BlockPos(23, 2, 18)), helper.getAbsoluteDirection(Direction.WEST));
        level.addFreshEntity(frame);
        BlockPos door = new BlockPos(20, 1, 23);
        door(helper, door, Blocks.OAK_DOOR);

        check(helper, force(helper, ghost, HauntingAction.SNUFF_LIGHT).isEmpty(), "snuffed a light with griefing off");
        check(helper, force(helper, ghost, HauntingAction.FLIP_LEVER).isEmpty(), "flipped a lever with griefing off");
        check(helper, force(helper, ghost, HauntingAction.KNOCK_OFF_DECORATION).isEmpty(), "knocked off a frame with griefing off");
        for (int i = 0; i < 20; i++) {
            Optional<HauntingController.Record> any = force(helper, ghost, null);
            check(helper, any.isEmpty() || !any.get().action().griefing(), "random pick chose griefing action %s", any.map(r -> r.action().id()).orElse(""));
        }
        helper.assertBlockPresent(Blocks.TORCH, new BlockPos(22, 1, 20));
        check(helper, !helper.getBlockState(new BlockPos(18, 1, 20)).getValue(LeverBlock.POWERED), "lever moved");
        check(helper, !frame.isRemoved(), "item frame broke");
        Optional<HauntingController.Record> swung = force(helper, ghost, HauntingAction.TOGGLE_OPENING);
        check(helper, swung.isPresent(), "door didn't swing even though doors aren't griefing");
        ghost.discard();
        frame.discard();
    }

    private static void griefingBlockedByGameRule(GameTestHelper helper) {

        griefingIsBlocked(helper);
        helper.succeed();
    }

    private static void griefingBlockedByConfig(GameTestHelper helper) {

        withConfig(ModConfigs.server().withHauntingGriefing(false), () -> griefingIsBlocked(helper));
        helper.succeed();
    }

    // ---- scheduling ----

    /**
     * With nobody within 16 blocks the controller never schedules or acts, even with a 1 s interval.
     */
    private static void idleWithoutPlayers(GameTestHelper helper) {

        ServerConfig original = ModConfigs.server();
        ModConfigs.setServerForTesting(original.withHauntingInterval(1, 1));
        LingeringSpiritEntity ghost = ghost(helper);
        BlockPos door = new BlockPos(22, 1, 20);
        door(helper, door, Blocks.OAK_DOOR);
        helper.runAfterDelay(80, () -> {
            ModConfigs.setServerForTesting(original);
            check(helper, ghost.haunting().history().isEmpty(), "haunted with no player in range: %s", ghost.haunting().history());
            check(helper, !ghost.haunting().schedule().isScheduled(), "scheduled a haunt with no player in range");
            check(helper, !open(helper, door), "door moved");
            ghost.discard();
            helper.succeed();
        });
    }

    /**
     * 1 s interval, a door and a wall and a player as targets: actions come at least 20 ticks apart, and no action
     * repeats within its 60 s cooldown, so over 5 s each of the three happens at most once.
     */
    private static void intervalAndCooldowns(GameTestHelper helper) {

        ServerConfig original = ModConfigs.server();
        ModConfigs.setServerForTesting(original.withHauntingInterval(1, 1));
        LingeringSpiritEntity ghost = ghost(helper);
        door(helper, new BlockPos(22, 1, 20), Blocks.OAK_DOOR);
        wall(helper, 17, 19, 21);
        ServerPlayer player = playerAt(helper, new BlockPos(23, 1, 22));
        helper.runAfterDelay(100, () -> {
            ModConfigs.setServerForTesting(original);
            removePlayer(player);
            List<HauntingController.Record> history = ghost.haunting().history().reversed();
            check(helper, history.size() >= 2, "only %s haunt(s) in 5 s with a 1 s interval", history.size());
            for (int i = 1; i < history.size(); i++) {
                long gap = history.get(i).gameTime() - history.get(i - 1).gameTime();
                check(helper, gap >= 20, "two haunts %s ticks apart", gap);
            }
            for (int i = 0; i < history.size(); i++) {
                for (int j = i + 1; j < history.size(); j++) {
                    boolean repeat = history.get(i).action() == history.get(j).action()
                            && history.get(j).gameTime() - history.get(i).gameTime() < HauntingSchedule.ACTION_COOLDOWN_TICKS;
                    check(helper, !repeat, "%s repeated inside its cooldown", history.get(i).action().id());
                }
            }
            ghost.discard();
            helper.succeed();
        });
    }

    private static void spikesEmfHolders(GameTestHelper helper) {

        LingeringSpiritEntity ghost = ghost(helper);
        door(helper, new BlockPos(22, 1, 20), Blocks.OAK_DOOR);
        ServerPlayer holder = playerAt(helper, new BlockPos(26, 1, 20));
        holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.EMF_READER.get()));
        ServerPlayer bystander = playerAt(helper, new BlockPos(14, 1, 20));
        check(helper, !ToolTracker.spikeActive(holder), "spike before any activity");
        check(helper, force(helper, ghost, HauntingAction.TOGGLE_OPENING).isPresent(), "door didn't swing");
        boolean holderSpiked = ToolTracker.spikeActive(holder);
        boolean bystanderSpiked = ToolTracker.spikeActive(bystander);
        removePlayer(holder);
        removePlayer(bystander);
        check(helper, holderSpiked, "the EMF holder wasn't spiked");
        check(helper, !bystanderSpiked, "a player without a reader was spiked");
        ghost.discard();
        helper.succeed();
    }
}
