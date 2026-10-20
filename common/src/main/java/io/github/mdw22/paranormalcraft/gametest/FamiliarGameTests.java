package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.binding.SpiritVesselItem;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.CapturedGhosts;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlockEntity;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.familiar.Familiars;
import io.github.mdw22.paranormalcraft.familiar.WispFamiliarEntity;
import io.github.mdw22.paranormalcraft.familiar.WispLights;
import io.github.mdw22.paranormalcraft.haunting.HauntingAction;
import io.github.mdw22.paranormalcraft.phase.Phasing;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import io.github.mdw22.paranormalcraft.ward.WardLanternBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Set;

/**
 * GameTests for Milestone 2 part C: binding and unbinding in the Containment Unit, the Wisp Familiar (summon, recall,
 * the vessel link, one per player, its light blocks, warnings, guide) and the Spectral Step (passable blocks, stone,
 * the end-of-phase extension and nudge, cooldown and durability), plus the ward flare. All but the flare share the
 * {@code familiar} batch ({@code Familiars} and {@code Phasing} are per-server state).
 * <p>
 * Mock players aren't ticked on every loader, so phase tests call {@code Phasing.tick} themselves.
 */
public final class FamiliarGameTests {

    private static final BlockPos CENTRE = new BlockPos(20, 1, 20);

    private FamiliarGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("bind_cycle", FamiliarGameTests::bindCycle);
        RegistrationHelper.testFunction("bind_refused_without_vessel_or_charge", FamiliarGameTests::bindRefused);
        RegistrationHelper.testFunction("familiar_summon_and_recall", FamiliarGameTests::summonAndRecall);
        RegistrationHelper.testFunction("familiar_recalled_when_vessel_leaves", FamiliarGameTests::recalledWhenVesselLeaves);
        RegistrationHelper.testFunction("familiar_recalled_on_logout", FamiliarGameTests::recalledOnLogout);
        RegistrationHelper.testFunction("familiar_one_per_player", FamiliarGameTests::onePerPlayer);
        RegistrationHelper.testFunction("wisp_light_in_the_dark_moves_and_clears", FamiliarGameTests::lightInTheDark);
        RegistrationHelper.testFunction("wisp_light_not_where_bright", FamiliarGameTests::noLightWhereBright);
        RegistrationHelper.testFunction("wisp_light_air_only_and_orphan_sweep", FamiliarGameTests::lightAirOnlyAndSweep);
        RegistrationHelper.testFunction("wisp_warns_behind_not_in_front", FamiliarGameTests::warnsBehind);
        RegistrationHelper.testFunction("wisp_guide_heads_for_cabin", FamiliarGameTests::guide);
        RegistrationHelper.testFunction("phase_walks_through_passables", FamiliarGameTests::phaseThroughPassables);
        RegistrationHelper.testFunction("phase_extends_inside_pane_then_nudges", FamiliarGameTests::phaseExtends);
        RegistrationHelper.testFunction("spectral_step_cooldown_and_durability", FamiliarGameTests::cooldownAndDurability);
        RegistrationHelper.testFunction("ward_flare_recoils_ghost", FamiliarGameTests::wardFlare);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    // ---- set-up ----

    // TODO(26.3): makeMockServerPlayerInLevel is deprecated; switch once a replacement exists.
    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos relative, float yaw) {

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
        player.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        player.setYHeadRot(yaw);
        return player;
    }

    private static void remove(ServerPlayer player) {

        WispFamiliarEntity wisp = Familiars.of(player);
        if (wisp != null) {
            Familiars.recall(wisp, "test_end");
        }
        Phasing.end(player, "test_end");
        player.level().getServer().getPlayerList().remove(player);
    }

    private static ItemStack boundVessel(GameTestHelper helper) {

        ItemStack vessel = new ItemStack(ModItems.SPIRIT_VESSEL.get());
        CapturedGhost ghost = CapturedGhosts.fresh(helper.getLevel(), helper.absolutePos(CENTRE), "test");
        if (ghost == null) {
            throw helper.assertionException(Component.literal("could not make a ghost"));
        }
        vessel.set(ModDataComponents.CAPTURED_GHOST.get(), ghost);
        return vessel;
    }

    /** A player holding a bound vessel, its wisp summoned. */
    private static WispFamiliarEntity summoned(GameTestHelper helper, ServerPlayer player) {

        ItemStack vessel = boundVessel(helper);
        player.getInventory().setItem(0, vessel);
        Familiars.toggle(player, vessel);
        WispFamiliarEntity wisp = Familiars.of(player);
        check(helper, wisp != null && wisp.isAlive(), "no wisp after summoning");
        return wisp;
    }

    /** A sealed stone room (interior {@code from}..{@code to}), dark inside. */
    private static void room(GameTestHelper helper, BlockPos from, BlockPos to, Block wall) {

        for (int x = from.getX() - 1; x <= to.getX() + 1; x++) {
            for (int y = from.getY() - 1; y <= to.getY() + 1; y++) {
                for (int z = from.getZ() - 1; z <= to.getZ() + 1; z++) {
                    boolean inside = x >= from.getX() && x <= to.getX() && y >= from.getY() && y <= to.getY() && z >= from.getZ() && z <= to.getZ();
                    helper.setBlock(new BlockPos(x, y, z), inside ? Blocks.AIR : wall);
                }
            }
        }
    }

    // ---- binding ----

    private static ContainmentUnitBlockEntity unit(GameTestHelper helper, BlockPos relative) {

        helper.setBlock(relative, ModBlocks.CONTAINMENT_UNIT.get());
        if (!(helper.getBlockEntity(relative, ContainmentUnitBlockEntity.class) instanceof ContainmentUnitBlockEntity unit)) {
            throw helper.assertionException(Component.literal("no Containment Unit block entity"));
        }
        return unit;
    }

    private static void bindCycle(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ContainmentUnitBlockEntity unit = unit(helper, CENTRE);
        ServerPlayer player = player(helper, CENTRE.south(2), 180.0F);
        unit.fillForDebug(level, 1);
        unit.setCharge(20);
        unit.setItem(ContainmentUnitBlockEntity.VESSEL_SLOT, new ItemStack(ModItems.SPIRIT_VESSEL.get()));
        String name = unit.stored().getFirst().ghost().name();
        check(helper, unit.bind(0, player), "bind didn't start");
        check(helper, unit.charge() == 15, "bind took %d charge, expected 5", 20 - unit.charge());
        check(helper, !unit.queueProcess(0, player) && !unit.release(0, player), "the binding ghost could be processed or released");
        check(helper, !unit.bind(0, player), "a second bind started");
        helper.runAfterDelay(2, () -> {
            check(helper, unit.bindTicks() > 0, "binding isn't progressing");
            unit.debugAdvanceBinding(ContainmentUnitBlockEntity.BIND_TICKS);
        });
        helper.runAfterDelay(5, () -> {
            ItemStack vessel = unit.getItem(ContainmentUnitBlockEntity.VESSEL_SLOT);
            check(helper, SpiritVesselItem.isBound(vessel), "the vessel isn't bound");
            CapturedGhost ghost = vessel.get(ModDataComponents.CAPTURED_GHOST.get());
            check(helper, ghost != null && ghost.name().equals(name), "the vessel holds %s, expected %s", ghost == null ? "nothing" : ghost.name(), name);
            check(helper, unit.stored().isEmpty(), "the ghost is still in storage");
            check(helper, unit.unbind(player), "unbind failed");
            check(helper, unit.stored().size() == 1 && unit.stored().getFirst().ghost().name().equals(name), "unbind didn't return %s", name);
            check(helper, SpiritVesselItem.isEmptyVessel(unit.getItem(ContainmentUnitBlockEntity.VESSEL_SLOT)), "the vessel wasn't emptied");
            check(helper, !unit.unbind(player), "unbound an empty vessel");
            remove(player);
            helper.succeed();
        });
    }

    private static void bindRefused(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        ContainmentUnitBlockEntity unit = unit(helper, CENTRE);
        ServerPlayer player = player(helper, CENTRE.south(2), 180.0F);
        unit.fillForDebug(level, 1);
        unit.setCharge(20);
        check(helper, !unit.bind(0, player), "bound without a vessel");
        unit.setItem(ContainmentUnitBlockEntity.VESSEL_SLOT, new ItemStack(ModItems.SPIRIT_VESSEL.get()));
        unit.setCharge(ContainmentUnitBlockEntity.BIND_CHARGE - 1);
        check(helper, !unit.bind(0, player), "bound with too little charge");
        unit.setCharge(20);
        check(helper, !unit.bind(3, player), "bound a ghost that isn't there");
        check(helper, unit.queueProcess(0, player), "set-up: couldn't queue");
        check(helper, !unit.bind(0, player), "bound a ghost queued for processing");
        remove(player);
        helper.succeed();
    }

    // ---- the familiar ----

    private static void summonAndRecall(GameTestHelper helper) {

        ServerPlayer player = player(helper, CENTRE, 0.0F);
        WispFamiliarEntity wisp = summoned(helper, player);
        check(helper, !wisp.isInvisible(), "the wisp is invisible");
        check(helper, !wisp.isPickable() && !wisp.isPushable(), "the wisp can be hit or pushed");
        helper.runAfterDelay(20, () -> {
            double distance = wisp.distanceTo(player);
            check(helper, distance >= 1.5 && distance <= 3.5, "the wisp is %.2f blocks from its owner, expected about 2 to 3", distance);
            check(helper, wisp.getY() - player.getY() > 1.0, "the wisp isn't at shoulder height (%.2f above the feet)", wisp.getY() - player.getY());
            Familiars.toggle(player, player.getInventory().getItem(0));
            check(helper, Familiars.of(player) == null && wisp.isRemoved(), "using the vessel again didn't recall the wisp");
            remove(player);
            helper.succeed();
        });
    }

    private static void recalledWhenVesselLeaves(GameTestHelper helper) {

        ServerPlayer player = player(helper, CENTRE, 0.0F);
        WispFamiliarEntity wisp = summoned(helper, player);
        helper.runAfterDelay(3, () -> {
            check(helper, Familiars.of(player) == wisp, "recalled too early");
            player.getInventory().setItem(0, ItemStack.EMPTY);
        });
        helper.runAfterDelay(6, () -> {
            check(helper, Familiars.of(player) == null && wisp.isRemoved(), "the wisp stayed out without its vessel");
            remove(player);
            helper.succeed();
        });
    }

    private static void recalledOnLogout(GameTestHelper helper) {

        ServerPlayer player = player(helper, CENTRE, 0.0F);
        WispFamiliarEntity wisp = summoned(helper, player);
        helper.runAfterDelay(3, () -> {
            // What the loaders' logout hooks call.
            io.github.mdw22.paranormalcraft.CommonEvents.onPlayerLogout(player);
            check(helper, Familiars.of(player) == null && wisp.isRemoved(), "the wisp stayed out after its owner logged out");
            remove(player);
            helper.succeed();
        });
    }

    private static void onePerPlayer(GameTestHelper helper) {

        ServerPlayer player = player(helper, CENTRE, 0.0F);
        WispFamiliarEntity first = summoned(helper, player);
        ItemStack second = boundVessel(helper);
        player.getInventory().setItem(1, second);
        Familiars.toggle(player, second);
        WispFamiliarEntity now = Familiars.of(player);
        check(helper, now != null && now != first, "the second vessel didn't summon its wisp");
        check(helper, first.isRemoved(), "the first wisp is still out");
        long wisps = helper.getLevel().getEntitiesOfClass(WispFamiliarEntity.class, player.getBoundingBox().inflate(16),
                wisp -> player.getUUID().equals(wisp.owner())).size();
        check(helper, wisps == 1, "%d wisps out for one player", wisps);
        remove(player);
        helper.succeed();
    }

    // ---- light ----

    private static void lightInTheDark(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        BlockPos from = new BlockPos(14, 2, 14);
        BlockPos to = new BlockPos(26, 6, 26);
        room(helper, from, to, Blocks.STONE);
        // Give the light engine a moment to darken the sealed room.
        helper.runAfterDelay(20, () -> {
            ServerPlayer player = player(helper, new BlockPos(20, 2, 18), 0.0F);
            WispFamiliarEntity wisp = summoned(helper, player);
            BlockPos[] first = new BlockPos[1];
            helper.runAfterDelay(30, () -> {
                first[0] = wisp.light();
                check(helper, first[0] != null, "no light in a dark room");
                check(helper, WispLights.isWispLight(level.getBlockState(first[0])), "the light block isn't there");
                Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(20, 2, 23)));
                player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            });
            helper.runAfterDelay(60, () -> {
                BlockPos moved = wisp.light();
                check(helper, moved != null && !moved.equals(first[0]), "the light didn't follow the wisp");
                check(helper, level.getBlockState(first[0]).isAir(), "the old light block stayed behind");
                Familiars.recall(wisp, "test");
                check(helper, level.getBlockState(moved).isAir(), "recall left the light block");
                check(helper, !WispLights.get(level.getServer()).describe().contains(level.dimension().identifier() + " " + moved.toShortString()),
                        "recall left the light listed");
                // Unload: summon again, then unload the wisp the way a chunk unload does.
                WispFamiliarEntity again = summoned(helper, player);
                helper.runAfterDelay(30, () -> {
                    BlockPos light = again.light();
                    check(helper, light != null, "no light the second time");
                    again.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                    check(helper, level.getBlockState(light).isAir(), "an unload left the light block");
                    check(helper, Familiars.of(player) == null, "an unloaded wisp is still listed as out");
                    remove(player);
                    helper.succeed();
                });
            });
        });
    }

    private static void noLightWhereBright(GameTestHelper helper) {

        BlockPos from = new BlockPos(14, 2, 14);
        BlockPos to = new BlockPos(26, 6, 26);
        room(helper, from, to, Blocks.GLOWSTONE);
        helper.runAfterDelay(20, () -> {
            ServerPlayer player = player(helper, new BlockPos(20, 2, 20), 0.0F);
            WispFamiliarEntity wisp = summoned(helper, player);
            helper.runAfterDelay(30, () -> {
                check(helper, wisp.light() == null, "a light in a bright room at %s", wisp.light());
                remove(player);
                helper.succeed();
            });
        });
    }

    private static void lightAirOnlyAndSweep(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        BlockPos stone = helper.absolutePos(new BlockPos(10, 1, 10));
        BlockPos water = helper.absolutePos(new BlockPos(12, 1, 10));
        BlockPos air = helper.absolutePos(new BlockPos(14, 3, 10));
        helper.setBlock(new BlockPos(10, 1, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(12, 1, 10), Blocks.WATER);
        check(helper, !WispLights.place(level, stone), "placed a light in stone");
        check(helper, !WispLights.place(level, water), "placed a light in water");
        check(helper, WispLights.place(level, air), "couldn't place a light in air");
        // No wisp owns it now: an orphan. The periodic sweep removes it.
        int removed = WispLights.sweepLoaded(level.getServer(), Set.of());
        check(helper, removed >= 1 && level.getBlockState(air).isAir(), "the sweep left the orphan light");
        // A light block the player has since replaced is kept.
        check(helper, WispLights.place(level, air), "couldn't place a second light");
        level.setBlock(air, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        WispLights.sweepLoaded(level.getServer(), Set.of());
        check(helper, level.getBlockState(air).is(Blocks.GLASS), "the sweep removed a block that wasn't the wisp's");
        helper.succeed();
    }

    // ---- warnings and guide ----

    private static void warnsBehind(GameTestHelper helper) {

        // Facing south (+z).
        ServerPlayer player = player(helper, CENTRE, 0.0F);
        WispFamiliarEntity wisp = summoned(helper, player);
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, CENTRE.north(6));
        helper.runAfterDelay(15, () -> {
            check(helper, wisp.isWarning(), "no warning for a husk straight behind");
            Vec3 front = helper.absoluteVec(Vec3.atBottomCenterOf(CENTRE.south(6)));
            husk.snapTo(front.x, front.y, front.z, 180.0F, 0.0F);
        });
        helper.runAfterDelay(30, () -> {
            check(helper, !wisp.isWarning(), "a warning for a husk in plain view");
            husk.discard();
            remove(player);
            helper.succeed();
        });
    }

    private static void guide(GameTestHelper helper) {

        // GameTest worlds generate no structures, so a cabin template is placed and the locator pointed at it.
        BlockPos cabin = helper.absolutePos(new BlockPos(20, 1, 34));
        helper.getLevel().getStructureManager().get(io.github.mdw22.paranormalcraft.Constants.id("abandoned_cabin_oak")).ifPresent(template ->
                template.placeInWorld(helper.getLevel(), cabin.offset(-7, 0, -2), cabin.offset(-7, 0, -2),
                        new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), helper.getLevel().getRandom(),
                        Block.UPDATE_CLIENTS));
        Familiars.setLocatorOverride((level, origin) -> cabin);
        ServerPlayer player = player(helper, CENTRE, 90.0F);
        WispFamiliarEntity wisp = summoned(helper, player);
        BlockPos found = Familiars.guide(player);
        check(helper, cabin.equals(found), "the guide found %s, expected the cabin at %s", found, cabin);
        Vec3 target = wisp.guideTarget();
        check(helper, target != null && wisp.isGuiding(), "the wisp isn't guiding");
        Vec3 offset = target.subtract(player.position());
        check(helper, Math.abs(Math.hypot(offset.x, offset.z) - Balance.Familiar.GUIDE_AHEAD) < 0.01 && offset.z > 7.9,
                "the wisp heads to %s, expected 8 blocks south toward the cabin", offset);
        check(helper, Familiars.guide(player) == null, "guided again during the cooldown");
        helper.runAfterDelay(30, () -> {
            check(helper, wisp.position().subtract(player.position()).z > 6.0, "the wisp didn't fly toward the cabin");
            Familiars.setLocatorOverride((level, origin) -> null);
            Familiars.resetGuideCooldown(player);
            check(helper, Familiars.guide(player) == null, "found a cabin with none in range");
            check(helper, wisp.isShaking(), "no shake for no cabin");
            Familiars.setLocatorOverride(null);
            remove(player);
            helper.succeed();
        });
    }

    // ---- Spectral Step ----

    /**
     * One lane per block: the wall is a row of it across z = 8, the player starts at z = 5.5 and walks 4 south. Through
     * the wall is z 9.5; the thinnest wall (bars, panes, centred) stops the player at about z 8.14.
     */
    private static void phaseThroughPassables(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        BlockState[] walls = {Blocks.IRON_BARS.defaultBlockState(), Blocks.GLASS_PANE.defaultBlockState(), door,
                Blocks.COBBLESTONE_WALL.defaultBlockState(), Blocks.GLASS.defaultBlockState(), Blocks.STONE.defaultBlockState()};
        boolean[] passes = {true, true, true, true, true, false};
        String[] names = {"iron bars", "a glass pane", "a closed oak door", "a cobblestone wall", "glass", "stone"};
        for (int lane = 0; lane < walls.length; lane++) {
            int x = 4 + lane * 5;
            for (int dx = -2; dx <= 2; dx++) {
                if (walls[lane].getBlock() instanceof DoorBlock) {
                    helper.setBlock(new BlockPos(x + dx, 1, 8), walls[lane].setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                    helper.setBlock(new BlockPos(x + dx, 2, 8), walls[lane].setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                } else {
                    helper.setBlock(new BlockPos(x + dx, 1, 8), walls[lane]);
                    helper.setBlock(new BlockPos(x + dx, 2, 8), walls[lane]);
                }
            }
        }
        helper.runAfterDelay(2, () -> {
            for (int lane = 0; lane < walls.length; lane++) {
                int x = 4 + lane * 5;
                ServerPlayer player = player(helper, new BlockPos(x, 1, 5), 0.0F);
                Vec3 start = helper.absoluteVec(new Vec3(x + 0.5, 1.0, 5.5));
                // Unphased, every wall stops the player.
                player.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
                player.move(MoverType.SELF, new Vec3(0, 0, 4));
                double blockedZ = player.getZ() - helper.absoluteVec(Vec3.ZERO).z;
                check(helper, blockedZ < 9.0, "walked through %s without phasing (z %.2f)", names[lane], blockedZ);
                Phasing.start(player);
                player.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
                player.move(MoverType.SELF, new Vec3(0, 0, 4));
                double phasedZ = player.getZ() - helper.absoluteVec(Vec3.ZERO).z;
                if (passes[lane]) {
                    check(helper, phasedZ > 9.4, "phased but stopped by %s (z %.2f)", names[lane], phasedZ);
                } else {
                    check(helper, phasedZ < 9.0, "phased through %s (z %.2f)", names[lane], phasedZ);
                }
                check(helper, Phasing.isPhased(player) && level.getBlockState(BlockPos.containing(player.position())).getFluidState().isEmpty(),
                        "set-up: not phased");
                remove(player);
            }
            helper.succeed();
        });
    }

    private static void phaseExtends(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        for (int dx = -2; dx <= 2; dx++) {
            helper.setBlock(new BlockPos(20 + dx, 1, 20), Blocks.GLASS_PANE);
            helper.setBlock(new BlockPos(20 + dx, 2, 20), Blocks.GLASS_PANE);
        }
        ServerPlayer player = player(helper, new BlockPos(20, 1, 20), 0.0F);
        Phasing.start(player, 2);
        helper.runAfterDelay(4, () -> {
            check(helper, Phasing.insidePassable(level, player.getBoundingBox()), "set-up: the player isn't inside the pane");
            Phasing.tick(player);
            Phasing.State state = Phasing.state(player);
            check(helper, state != null && state.extended() > 0, "the phase ended inside a pane instead of extending");
            // Step out: the phase ends at once.
            Vec3 out = helper.absoluteVec(new Vec3(20.5, 1.0, 22.5));
            player.snapTo(out.x, out.y, out.z, 0.0F, 0.0F);
            Phasing.tick(player);
            check(helper, Phasing.state(player) == null, "the phase didn't end once clear");
            // Stay inside: after the extension runs out, a nudge to a free spot.
            Vec3 in = helper.absoluteVec(new Vec3(20.5, 1.0, 20.5));
            player.snapTo(in.x, in.y, in.z, 0.0F, 0.0F);
            Phasing.start(player, 1);
        });
        helper.runAfterDelay(8, () -> {
            for (int i = 0; i <= Phasing.MAX_EXTEND_TICKS + 1 && Phasing.state(player) != null; i++) {
                Phasing.tick(player);
            }
            check(helper, Phasing.state(player) == null, "still phased after the extension ran out");
            check(helper, !Phasing.insidePassable(level, player.getBoundingBox()), "not nudged out of the pane");
            remove(player);
            helper.succeed();
        });
    }

    private static void cooldownAndDurability(GameTestHelper helper) {

        ServerPlayer player = player(helper, CENTRE, 0.0F);
        ItemStack charm = new ItemStack(ModItems.SPECTRAL_STEP.get());
        check(helper, charm.getMaxDamage() == Balance.Phase.USES, "%d uses, expected 20", charm.getMaxDamage());
        player.setItemInHand(InteractionHand.MAIN_HAND, charm);
        InteractionResult result = charm.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(helper, result.consumesAction() && Phasing.isPhased(player), "using the charm didn't phase the player");
        check(helper, player.getCooldowns().isOnCooldown(charm), "no cooldown after use");
        check(helper, charm.getDamageValue() == 1, "used %d durability, expected 1", charm.getDamageValue());
        check(helper, !charm.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "used again while phased");
        Phasing.end(player, "test");
        ItemStack worn = new ItemStack(ModItems.SPECTRAL_STEP.get());
        worn.setDamageValue(Balance.Phase.USES - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, worn);
        worn.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(helper, player.getMainHandItem().isEmpty(), "the charm didn't break on its last use");
        check(helper, new ItemStack(ModItems.SPECTRAL_STEP.get()).isValidRepairItem(new ItemStack(ModItems.LINGERING_ECTOPLASM.get())),
                "not repairable with Lingering Ectoplasm");
        remove(player);
        helper.succeed();
    }

    // ---- ward flare (containment batch) ----

    private static void wardFlare(GameTestHelper helper) {

        ServerLevel level = helper.getLevel();
        BlockPos ghostAt = new BlockPos(20, 2, 20);
        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), ghostAt);
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(ghostAt.offset(2, -1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(ghostAt.offset(2, 0, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos lantern = ghostAt.offset(-6, -1, 0);
        helper.setBlock(lantern, ModBlocks.WARD_LANTERN.get());
        check(helper, ghost.haunting().force(level, HauntingAction.TOGGLE_OPENING).isEmpty(), "haunted inside a ward");
        check(helper, helper.getBlockState(lantern).getValue(WardLanternBlock.FLARE), "the lantern didn't flare");
        check(helper, ghost.getDeltaMovement().x > 0.5, "the ghost wasn't pushed away from the lantern (%s)", ghost.getDeltaMovement());
        check(helper, level.getLightEmission(helper.absolutePos(lantern)) == Balance.Ward.FLARE_LIGHT, "the flare isn't light 15");
        helper.runAfterDelay(Balance.Ward.FLARE_TICKS + 4, () -> {
            check(helper, !helper.getBlockState(lantern).getValue(WardLanternBlock.FLARE), "the flare didn't fade");
            ghost.discard();
            helper.succeed();
        });
    }
}
