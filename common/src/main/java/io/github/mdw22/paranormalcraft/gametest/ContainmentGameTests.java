package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.capture.CapturedGhosts;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlock;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlockEntity;
import io.github.mdw22.paranormalcraft.containment.StoredGhost;
import io.github.mdw22.paranormalcraft.effect.RestfulEffect;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationScheduler;
import io.github.mdw22.paranormalcraft.haunting.HauntingAction;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.journal.JournalEvents;
import io.github.mdw22.paranormalcraft.journal.JournalStages;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEffects;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import io.github.mdw22.paranormalcraft.ward.Wards;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * GameTests for Milestone 2 part B: the Containment Unit (load, capacity, charge, fuel, Unstable, processing,
 * release, hoppers, drops, persistence), the Ward Lantern (spawns, haunting, hallucinations) and the Field Journal's
 * advancement triggers. Containment and ward tests share the {@code containment} batch (processing rolls are a static
 * override); journal tests change the time of day, so they run in the {@code journal} batch.
 */
public final class ContainmentGameTests {

    private static final BlockPos UNIT = new BlockPos(10, 2, 10);
    private static final BlockPos CENTRE = new BlockPos(20, 2, 20);

    private ContainmentGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("containment_loads_filled_trap", ContainmentGameTests::loadsFilledTrap);
        RegistrationHelper.testFunction("containment_capacity_four", ContainmentGameTests::capacityFour);
        RegistrationHelper.testFunction("containment_charge_drains_per_ghost", ContainmentGameTests::chargeDrainsPerGhost);
        RegistrationHelper.testFunction("containment_fuel_at_or_below_80", ContainmentGameTests::fuelAtOrBelow80);
        RegistrationHelper.testFunction("containment_unstable_pauses_processing", ContainmentGameTests::unstablePausesProcessing);
        RegistrationHelper.testFunction("containment_process_yield_low_roll", helper -> processYield(helper, 0.0, 3, true));
        RegistrationHelper.testFunction("containment_process_yield_high_roll", helper -> processYield(helper, 0.99, 5, false));
        RegistrationHelper.testFunction("containment_release_blesses", ContainmentGameTests::releaseBlesses);
        RegistrationHelper.testFunction("containment_hoppers", ContainmentGameTests::hoppers);
        RegistrationHelper.testFunction("containment_break_drops_filled_traps", ContainmentGameTests::breakDropsFilledTraps);
        RegistrationHelper.testFunction("containment_save_and_reload", ContainmentGameTests::saveAndReload);
        RegistrationHelper.testFunction("ward_blocks_natural_monster_spawns", ContainmentGameTests::wardBlocksSpawns);
        RegistrationHelper.testFunction("ward_blocks_haunting", ContainmentGameTests::wardBlocksHaunting);
        RegistrationHelper.testFunction("ward_blocks_hallucinations", ContainmentGameTests::wardBlocksHallucinations);
        RegistrationHelper.testFunction("journal_capture_triggers", ContainmentGameTests::journalCapture);
        RegistrationHelper.testFunction("journal_containment_triggers", ContainmentGameTests::journalContainment);
        RegistrationHelper.testFunction("journal_looking_triggers", ContainmentGameTests::journalLooking);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    // ---- set-up ----

    private static ContainmentUnitBlockEntity unit(GameTestHelper helper, BlockPos relative) {

        helper.setBlock(relative.below(), Blocks.STONE);
        helper.setBlock(relative, ModBlocks.CONTAINMENT_UNIT.get());
        if (!(helper.getBlockEntity(relative, ContainmentUnitBlockEntity.class) instanceof ContainmentUnitBlockEntity unit)) {
            throw helper.assertionException(Component.literal("no Containment Unit block entity"));
        }
        return unit;
    }

    private static ItemStack filledTrap(GameTestHelper helper) {

        return CapturedGhosts.filledTrap(helper.getLevel(), helper.absolutePos(CENTRE), "test");
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

    private static int count(ContainmentUnitBlockEntity unit, net.minecraft.world.item.Item item) {

        int total = 0;
        for (int slot = ContainmentUnitBlockEntity.OUTPUT_START; slot <= ContainmentUnitBlockEntity.OUTPUT_END; slot++) {
            if (unit.getItem(slot).is(item)) {
                total += unit.getItem(slot).getCount();
            }
        }
        return total;
    }

    private static boolean advancementDone(ServerPlayer player, String path) {

        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Constants.id(path));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    // ---- containment ----

    /**
     * A Filled Trap in the input moves its ghost into storage and comes back out as an empty trap.
     */
    private static void loadsFilledTrap(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        ItemStack trap = filledTrap(helper);
        String name = trap.get(ModDataComponents.CAPTURED_GHOST.get()).name();
        unit.setItem(ContainmentUnitBlockEntity.INPUT_SLOT, trap);
        helper.succeedWhen(() -> {
            check(helper, unit.stored().size() == 1 && unit.stored().getFirst().ghost().name().equals(name), "stored %s", unit.stored());
            check(helper, unit.getItem(ContainmentUnitBlockEntity.INPUT_SLOT).isEmpty(), "the input wasn't taken");
            check(helper, count(unit, ModItems.GHOST_TRAP.get()) == 1, "no empty trap in the output");
            check(helper, helper.getBlockState(UNIT).getValue(ContainmentUnitBlock.GHOSTS) == 1, "block state ghosts %d",
                    helper.getBlockState(UNIT).getValue(ContainmentUnitBlock.GHOSTS));
        });
    }

    /**
     * Four fit; the fifth Filled Trap stays in the input.
     */
    private static void capacityFour(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        check(helper, unit.fillForDebug(helper.getLevel(), 6) == ContainmentUnitBlockEntity.CAPACITY, "debug fill went past capacity");
        unit.setItem(ContainmentUnitBlockEntity.INPUT_SLOT, filledTrap(helper));
        helper.runAfterDelay(10, () -> {
            check(helper, unit.stored().size() == 4, "%d stored", unit.stored().size());
            check(helper, !unit.getItem(ContainmentUnitBlockEntity.INPUT_SLOT).isEmpty(), "the fifth trap was taken");
            check(helper, ContainmentUnitBlock.comparatorSignal(4) == 15 && ContainmentUnitBlock.comparatorSignal(2) == 8,
                    "comparator signal %d for 4", ContainmentUnitBlock.comparatorSignal(4));
            helper.succeed();
        });
    }

    /**
     * Four ghosts drain 2 points in 300 ticks (1 point per ghost per 600 ticks).
     */
    private static void chargeDrainsPerGhost(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        unit.fillForDebug(helper.getLevel(), 4);
        unit.setCharge(100);
        helper.runAfterDelay(301, () -> {
            check(helper, unit.charge() == 98, "charge %d after 300 ticks with 4 ghosts, expected 98", unit.charge());
            helper.succeed();
        });
    }

    /**
     * Fuel is taken only at 80 or below, adding 20.
     */
    private static void fuelAtOrBelow80(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        unit.setCharge(81);
        unit.setItem(ContainmentUnitBlockEntity.FUEL_SLOT, new ItemStack(Items.SOUL_SAND, 2));
        helper.runAfterDelay(5, () -> {
            check(helper, unit.charge() == 81 && unit.getItem(ContainmentUnitBlockEntity.FUEL_SLOT).getCount() == 2, "fuel used above 80");
            unit.setCharge(80);
        });
        helper.runAfterDelay(8, () -> {
            check(helper, unit.charge() == 100, "charge %d after fuelling at 80", unit.charge());
            check(helper, unit.getItem(ContainmentUnitBlockEntity.FUEL_SLOT).getCount() == 1, "fuel not consumed");
            check(helper, !ContainmentUnitBlockEntity.isFuel(new ItemStack(Items.SAND)) && ContainmentUnitBlockEntity.isFuel(new ItemStack(Items.SOUL_SOIL)),
                    "fuel filter");
            helper.succeed();
        });
    }

    /**
     * At 0 charge with ghosts inside the unit is Unstable and processing stops; fuel brings it back.
     */
    private static void unstablePausesProcessing(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        ServerPlayer player = player(helper, UNIT.offset(2, -1, 0));
        unit.fillForDebug(helper.getLevel(), 1);
        unit.setCharge(0);
        check(helper, unit.queueProcess(0, player), "couldn't queue");
        int[] progress = new int[1];
        helper.runAfterDelay(3, () -> {
            check(helper, unit.isUnstable(), "not Unstable at 0 charge with a ghost");
            check(helper, helper.getBlockState(UNIT).getValue(ContainmentUnitBlock.UNSTABLE), "block state not Unstable");
            progress[0] = unit.processTicks();
        });
        helper.runAfterDelay(23, () -> {
            check(helper, unit.processTicks() == progress[0], "processing moved while Unstable (%d -> %d)", progress[0], unit.processTicks());
            unit.setItem(ContainmentUnitBlockEntity.FUEL_SLOT, new ItemStack(Items.SOUL_SAND));
        });
        helper.runAfterDelay(30, () -> {
            check(helper, !unit.isUnstable() && unit.charge() > 0, "still Unstable after fuel (charge %d)", unit.charge());
            check(helper, unit.processTicks() > progress[0], "processing didn't resume");
            remove(player);
            helper.succeed();
        });
    }

    /**
     * Processing yields 3–5 Lingering Ectoplasm (seeded) and, on a low roll, a Spectral Residue.
     */
    private static void processYield(GameTestHelper helper, double roll, int ectoplasm, boolean residue) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        ServerPlayer player = player(helper, UNIT.offset(2, -1, 0));
        unit.fillForDebug(helper.getLevel(), 2);
        unit.setCharge(100);
        unit.setRollOverride(() -> roll);
        check(helper, unit.queueProcess(0, player) && unit.queueProcess(1, player), "couldn't queue");
        helper.runAfterDelay(2, () -> unit.debugAdvanceProcessing(ContainmentUnitBlockEntity.PROCESS_TICKS));
        helper.runAfterDelay(6, () -> {
            check(helper, unit.stored().size() == 1, "%d stored after processing one", unit.stored().size());
            check(helper, count(unit, ModItems.LINGERING_ECTOPLASM.get()) == ectoplasm, "%d ectoplasm, expected %d",
                    count(unit, ModItems.LINGERING_ECTOPLASM.get()), ectoplasm);
            check(helper, (count(unit, ModItems.SPECTRAL_RESIDUE.get()) == 1) == residue, "residue %d", count(unit, ModItems.SPECTRAL_RESIDUE.get()));
            check(helper, unit.queue().size() == 1 && unit.processTicks() <= 4, "the next ghost isn't processing (queue %d, ticks %d)",
                    unit.queue().size(), unit.processTicks());
            remove(player);
            helper.succeed();
        });
    }

    /**
     * Release: the ghost rises and fades for everyone, the releasing player gets Restful for 3 days, and Restful keeps
     * resetting the time-since-rest statistic phantoms spawn from.
     */
    private static void releaseBlesses(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        ServerPlayer player = player(helper, UNIT.offset(2, -1, 0));
        unit.fillForDebug(helper.getLevel(), 1);
        String name = unit.stored().getFirst().ghost().name();
        check(helper, unit.release(0, player), "release refused");
        check(helper, unit.stored().isEmpty(), "still stored");
        MobEffectInstance restful = player.getEffect(RegistrationHelper.holder(ModEffects.RESTFUL));
        check(helper, restful != null && restful.getDuration() == 72_000, "Restful %s", restful);
        GhostEntity ghost = helper.getLevel().getEntitiesOfClass(GhostEntity.class, new AABB(helper.absolutePos(UNIT)).inflate(3)).stream()
                .filter(found -> found.ghostName().equals(name)).findFirst().orElse(null);
        check(helper, ghost != null && ghost.isAscending(), "no ascending ghost named %s", name);
        player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), 200_000);
        new RestfulEffect().applyEffectTick(helper.getLevel(), player, 0);
        int sinceRest = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        check(helper, sinceRest == 0, "time since rest %d after a Restful tick", sinceRest);
        helper.runAfterDelay(70, () -> {
            check(helper, ghost.isRemoved(), "the ghost didn't finish ascending");
            remove(player);
            helper.succeed();
        });
    }

    /**
     * Hoppers: soul sand from the side fuels it; outputs leave from the bottom.
     */
    private static void hoppers(GameTestHelper helper) {

        BlockPos unitPos = new BlockPos(10, 3, 10);
        helper.setBlock(new BlockPos(10, 1, 10), Blocks.CHEST);
        helper.setBlock(new BlockPos(10, 2, 10), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(unitPos, ModBlocks.CONTAINMENT_UNIT.get());
        ContainmentUnitBlockEntity unit = helper.getBlockEntity(unitPos, ContainmentUnitBlockEntity.class);
        helper.setBlock(new BlockPos(11, 3, 10), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        if (helper.getBlockEntity(new BlockPos(11, 3, 10), net.minecraft.world.level.block.entity.HopperBlockEntity.class)
                instanceof net.minecraft.world.level.block.entity.HopperBlockEntity hopper) {
            hopper.setItem(0, new ItemStack(Items.SOUL_SAND, 2));
            hopper.setItem(1, new ItemStack(Items.DIRT, 1));
        }
        unit.setCharge(40);
        unit.setItem(ContainmentUnitBlockEntity.OUTPUT_START, new ItemStack(ModItems.LINGERING_ECTOPLASM.get(), 3));
        helper.succeedWhen(() -> {
            check(helper, unit.charge() >= 60, "charge %d: no fuel came in from the side", unit.charge());
            ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(10, 1, 10), ChestBlockEntity.class);
            int moved = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                if (chest.getItem(slot).is(ModItems.LINGERING_ECTOPLASM.get())) {
                    moved += chest.getItem(slot).getCount();
                }
                check(helper, !chest.getItem(slot).is(Items.SOUL_SAND), "fuel came out of the bottom");
            }
            check(helper, moved == 3, "%d ectoplasm reached the chest below", moved);
            check(helper, !unit.getItem(ContainmentUnitBlockEntity.FUEL_SLOT).is(Items.DIRT), "dirt got into the fuel slot");
        });
    }

    /**
     * Breaking the unit drops each stored ghost as a Filled Ghost Trap with its data, never a loose ghost.
     */
    private static void breakDropsFilledTraps(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        unit.fillForDebug(helper.getLevel(), 2);
        List<String> names = unit.stored().stream().map(ghost -> ghost.ghost().name()).toList();
        helper.getLevel().destroyBlock(helper.absolutePos(UNIT), true);
        helper.runAfterDelay(2, () -> {
            List<ItemEntity> traps = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(UNIT)).inflate(3),
                    item -> item.getItem().is(ModItems.FILLED_GHOST_TRAP.get()));
            List<String> dropped = traps.stream().map(item -> item.getItem().get(ModDataComponents.CAPTURED_GHOST.get()))
                    .filter(java.util.Objects::nonNull).map(captured -> captured.name()).sorted().toList();
            check(helper, dropped.equals(names.stream().sorted().toList()), "dropped %s, stored %s", dropped, names);
            check(helper, helper.getLevel().getEntitiesOfClass(GhostEntity.class, new AABB(helper.absolutePos(UNIT)).inflate(4)).isEmpty(),
                    "a loose ghost appeared");
            helper.succeed();
        });
    }

    /**
     * Stored ghosts, the queue and the charge survive a save and load.
     */
    private static void saveAndReload(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        ServerPlayer player = player(helper, UNIT.offset(2, -1, 0));
        unit.fillForDebug(helper.getLevel(), 3);
        unit.setCharge(57);
        unit.queueProcess(1, player);
        CompoundTag saved = unit.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.setBlock(UNIT.offset(3, 0, 0).below(), Blocks.STONE);
        helper.setBlock(UNIT.offset(3, 0, 0), ModBlocks.CONTAINMENT_UNIT.get());
        ContainmentUnitBlockEntity copy = helper.getBlockEntity(UNIT.offset(3, 0, 0), ContainmentUnitBlockEntity.class);
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            copy.loadWithComponents(TagValueInput.create(reporter, helper.getLevel().registryAccess(), saved));
        }
        check(helper, copy.stored().stream().map(StoredGhost::id).toList().equals(unit.stored().stream().map(StoredGhost::id).toList()),
                "stored ghosts differ after reload");
        check(helper, copy.charge() == 57 && copy.queue().equals(unit.queue()), "charge %d queue %s", copy.charge(), copy.queue());
        check(helper, copy.stored().get(1).queuedBy().isPresent(), "who queued it was lost");
        remove(player);
        helper.succeed();
    }

    // ---- ward ----

    /**
     * Natural monster spawn checks fail inside a ward and pass outside; other categories aren't affected. Calls the
     * natural spawner's own (private) position check, which the ward mixin hooks.
     */
    private static void wardBlocksSpawns(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        // A dark stone room so a zombie could spawn there.
        for (int x = 2; x <= 6; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = 2; z <= 6; z++) {
                    boolean shell = x == 2 || x == 6 || y == 4 || z == 2 || z == 6;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        // Lighting catches up a few ticks after the room is built.
        helper.runAfterDelay(10, () -> {
            BlockPos spot = helper.absolutePos(new BlockPos(4, 1, 4));
            Vec3 spotCentre = Vec3.atBottomCenterOf(spot);
            check(helper, Wards.blocksNaturalSpawn(level, MobCategory.MONSTER, spot) == Wards.covers(level, spotCentre), "ward check disagrees");
            boolean before = spawnCheck(helper, spot);
            helper.setBlock(new BlockPos(16, 1, 16), ModBlocks.WARD_LANTERN.get());
            check(helper, Wards.covers(level, spotCentre), "the ward doesn't cover the room");
            check(helper, Wards.blocksNaturalSpawn(level, MobCategory.MONSTER, spot), "monsters not blocked inside the ward");
            check(helper, !Wards.blocksNaturalSpawn(level, MobCategory.CREATURE, spot), "animals blocked inside the ward");
            check(helper, !spawnCheck(helper, spot), "the natural spawner still allows a zombie inside the ward");
            Constants.LOG.info("Ward spawn check: zombie allowed without a ward = {}, with a ward = false", before);
            check(helper, before, "the natural spawner refused a zombie in a dark room without a ward (test set-up)");
            helper.setBlock(new BlockPos(16, 1, 16), Blocks.AIR);
            check(helper, !Wards.covers(level, spotCentre), "the ward is still registered after breaking");
            helper.succeed();
        });
    }

    private static boolean spawnCheck(GameTestHelper helper, BlockPos spot) {

        ServerLevel level = helper.getLevel();
        try {
            Method method = NaturalSpawner.class.getDeclaredMethod("isValidSpawnPostitionForType", ServerLevel.class, MobCategory.class,
                    StructureManager.class, ChunkGenerator.class, MobSpawnSettings.SpawnerData.class, BlockPos.MutableBlockPos.class, double.class);
            method.setAccessible(true);
            // The biome's own zombie entry: the spawner checks the entry is one of the biome's.
            MobSpawnSettings.SpawnerData zombie = level.getBiome(spot).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                    .map(entry -> entry.value()).filter(data -> data.type() == EntityTypes.ZOMBIE).findFirst()
                    .orElseThrow(() -> helper.assertionException(Component.literal("no zombies in this biome")));
            return (boolean) method.invoke(null, level, MobCategory.MONSTER, level.structureManager(), level.getChunkSource().getGenerator(),
                    zombie, spot.mutable(), 40.0 * 40.0);
        } catch (ReflectiveOperationException e) {
            throw helper.assertionException(Component.literal("couldn't call the natural spawn check: " + e));
        }
    }

    /**
     * A ghost inside a ward does no haunting, even forced; outside it does.
     */
    private static void wardBlocksHaunting(GameTestHelper helper) {

        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE);
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(CENTRE.offset(2, -1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(CENTRE.offset(2, 0, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        check(helper, ghost.haunting().force(helper.getLevel(), HauntingAction.TOGGLE_OPENING).isPresent(), "no haunting without a ward (set-up)");
        helper.setBlock(CENTRE.offset(-6, -1, 0), ModBlocks.WARD_LANTERN.get());
        check(helper, ghost.haunting().force(helper.getLevel(), HauntingAction.TOGGLE_OPENING).isEmpty(), "haunted inside a ward");
        ghost.discard();
        helper.succeed();
    }

    /**
     * A player near a ghost in the dark gets no shadow figure inside a ward.
     */
    private static void wardBlocksHallucinations(GameTestHelper helper) {

        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE);
        ServerPlayer player = player(helper, CENTRE.offset(0, -1, -5));
        check(helper, HallucinationScheduler.conditionsMet(player), "no hallucination without a ward (set-up)");
        helper.setBlock(CENTRE.offset(0, -1, -8), ModBlocks.WARD_LANTERN.get());
        check(helper, !HallucinationScheduler.conditionsMet(player), "a hallucination would be sent inside a ward");
        remove(player);
        ghost.discard();
        helper.succeed();
    }

    // ---- journal ----

    /**
     * A capture fires the captor's Gotcha! and "captured" stage and logs it.
     */
    private static void journalCapture(GameTestHelper helper) {

        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE);
        ghost.setPresence(helper.getLevel(), 6.0F);
        ServerPlayer player = player(helper, CENTRE.offset(0, -1, -6));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TETHER_WAND.get()));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, ghost.getBoundingBox().getCenter());
        JournalStages.setStage(player, "paranormalcraft:lingering_spirit", 0);
        TetherManager.setForced(player, true);
        helper.runAfterDelay(3, () -> helper.setBlock(CENTRE.below(), ModBlocks.GHOST_TRAP.get()));
        helper.succeedWhen(() -> {
            check(helper, ghost.isRemoved(), "not captured yet");
            check(helper, advancementDone(player, "main/gotcha"), "Gotcha! not awarded");
            check(helper, player.getAdvancements().getOrStartProgress(player.level().getServer().getAdvancements()
                    .get(JournalStages.advancementId("paranormalcraft:lingering_spirit", 3))).isDone(), "\"captured\" stage not done");
            List<CaptureLog.Entry> log = CaptureLog.get(helper.getLevel().getServer()).entriesOf(player.getUUID());
            check(helper, !log.isEmpty() && log.getLast().ghost().equals(ghost.getUUID()) && log.getLast().outcome().equals("stored"), "log %s", log);
            TetherManager.setForced(player, false);
            remove(player);
        });
    }

    /**
     * Loading, processing and releasing fire Lockup, Ectoplasmic (+ studied) and Rest Easy, and update the log.
     */
    private static void journalContainment(GameTestHelper helper) {

        ContainmentUnitBlockEntity unit = unit(helper, UNIT);
        ServerPlayer player = player(helper, UNIT.offset(2, -1, 0));
        JournalStages.setStage(player, "paranormalcraft:lingering_spirit", 0);
        unit.setCharge(100);
        unit.setItem(ContainmentUnitBlockEntity.INPUT_SLOT, filledTrap(helper));
        unit.setItem(ContainmentUnitBlockEntity.FUEL_SLOT, ItemStack.EMPTY);
        helper.runAfterDelay(3, () -> {
            check(helper, unit.stored().size() == 1, "not loaded");
            check(helper, advancementDone(player, "main/lockup"), "Lockup not awarded (nearest player)");
            unit.fillForDebug(helper.getLevel(), 1);
            check(helper, unit.queueProcess(0, player), "couldn't queue");
            unit.debugAdvanceProcessing(ContainmentUnitBlockEntity.PROCESS_TICKS);
        });
        helper.runAfterDelay(6, () -> {
            check(helper, advancementDone(player, "main/ectoplasmic"), "Ectoplasmic not awarded");
            check(helper, player.getAdvancements().getOrStartProgress(player.level().getServer().getAdvancements()
                    .get(JournalStages.advancementId("paranormalcraft:lingering_spirit", 4))).isDone(), "\"studied\" not done after processing");
            check(helper, unit.release(0, player), "release refused");
            check(helper, advancementDone(player, "main/rest_easy"), "Rest Easy not awarded");
            remove(player);
            helper.succeed();
        });
    }

    /**
     * Detected (EMF 5 by a ghost), seen (goggles, looking at it) and Cold Feet (thermometer below 5° at its cold spot,
     * at night) from the per-tick checks.
     */
    private static void journalLooking(GameTestHelper helper) {

        helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), "time set midnight");
        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE);
        ServerPlayer player = player(helper, CENTRE.offset(0, -1, -3));
        JournalStages.setStage(player, "paranormalcraft:lingering_spirit", 0);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EMF_READER.get()));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.SPIRIT_THERMOMETER.get()));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, ghost.getEyePosition());
        helper.runAfterDelay(2, () -> {
            ghost.setHome(ghost.blockPosition(), helper.absolutePos(CENTRE.offset(0, -1, -3)));
            JournalEvents.check(player);
            check(helper, advancementDone(player, "main/somethings_here"), "Something's Here not awarded");
            check(helper, advancementDone(player, "main/seeing_things"), "Seeing Things not awarded");
            check(helper, JournalStages.stage(player, "paranormalcraft:lingering_spirit") == 2, "stage %d, expected 2",
                    JournalStages.stage(player, "paranormalcraft:lingering_spirit"));
            check(helper, advancementDone(player, "main/cold_feet"), "Cold Feet not awarded");
            remove(player);
            ghost.discard();
            helper.succeed();
        });
    }
}
