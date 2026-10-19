package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Abandoned Cabin templates are walkable: from the fence-line trail, onto the porch step, through the front door,
 * across the front room and through the partition door into the back room, every step rises at most half a block
 * and has 2 clear blocks above it. Doors count as open (a player opens them).
 */
public final class CabinGameTests {

    /** The route, in house coordinates (x 0..8, z 0..6, front at z=6). */
    private static final int[][] ROUTE = {
            {2, 10}, {2, 9}, {2, 8}, {2, 7}, {2, 6}, {2, 5}, {3, 5}, {4, 5}, {4, 4}, {4, 3}, {5, 3}, {6, 3}, {6, 2}};
    /** Where the template's corner goes in the test arena; the house starts 3 blocks in. */
    private static final BlockPos CORNER = new BlockPos(10, 0, 10);
    private static final double MAX_RISE = 0.5;
    private static final double HEADROOM = 2.0;

    private CabinGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("cabin_walkable_oak", helper -> walkable(helper, "oak"));
        RegistrationHelper.testFunction("cabin_walkable_spruce", helper -> walkable(helper, "spruce"));
    }

    private static void walkable(GameTestHelper helper, String wood) {

        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager().getOrCreate(Constants.id("abandoned_cabin_" + wood));
        BlockPos corner = helper.absolutePos(CORNER);
        template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        BlockPos house = corner.offset(3, 0, 3);
        List<String> steps = new ArrayList<>();
        Double previous = null;
        for (int[] cell : ROUTE) {
            BlockPos column = house.offset(cell[0], 0, cell[1]);
            Double surface = surface(level, column);
            if (surface == null) {
                throw fail(helper, "house (%d, %d): nothing to stand on", cell[0], cell[1]);
            }
            String blocked = headroomBlocker(level, column, surface);
            if (blocked != null) {
                throw fail(helper, "house (%d, %d): standing at %.2f, %s is in the way (needs %.0f blocks clear)", cell[0], cell[1], surface, blocked, HEADROOM);
            }
            if (previous != null && surface - previous > MAX_RISE + 1.0E-6) {
                throw fail(helper, "house (%d, %d): a %.2f step up from %.2f", cell[0], cell[1], surface - previous, previous);
            }
            steps.add(String.format(Locale.ROOT, "(%d,%d)=%.2f", cell[0], cell[1], surface));
            previous = surface;
        }
        Constants.LOG.info("Cabin {} route: {}", wood, String.join(" ", steps));
        helper.succeed();
    }

    private static RuntimeException fail(GameTestHelper helper, String message, Object... args) {

        return helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
    }

    private static boolean passable(BlockState state) {

        return state.getBlock() instanceof DoorBlock;
    }

    /**
     * The height (relative to the template's ground layer, y=0) a player stands at in this column: the top of the
     * highest collision box from y=2 down.
     */
    private static Double surface(ServerLevel level, BlockPos column) {

        for (int dy = 2; dy >= -1; dy--) {
            BlockPos pos = column.above(dy);
            BlockState state = level.getBlockState(pos);
            if (passable(state)) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return dy + shape.max(Direction.Axis.Y);
            }
        }
        return null;
    }

    /**
     * What blocks the space from {@code surface} to {@code surface + 2} in this column, or null if it's clear.
     */
    private static String headroomBlocker(ServerLevel level, BlockPos column, double surface) {

        double top = surface + HEADROOM;
        for (int dy = (int) Math.floor(surface); dy <= (int) Math.floor(top - 1.0E-6); dy++) {
            BlockPos pos = column.above(dy);
            BlockState state = level.getBlockState(pos);
            if (passable(state)) {
                continue;
            }
            for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
                if (box.minY + dy < top - 1.0E-6 && box.maxY + dy > surface + 1.0E-6) {
                    return state.getBlock().getName().getString() + " at y+" + dy;
                }
            }
        }
        return null;
    }
}
