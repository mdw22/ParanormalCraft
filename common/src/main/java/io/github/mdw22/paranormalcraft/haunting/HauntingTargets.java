package io.github.mdw22.paranormalcraft.haunting;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a ghost could haunt right now: blocks within {@link #RANGE}, paintings and item frames, loose items within
 * {@link #ITEM_RANGE} and players to follow. One block scan serves every action.
 */
public final class HauntingTargets {

    public static final int RANGE = Balance.Haunting.BLOCK_RANGE;
    public static final int ITEM_RANGE = Balance.Haunting.ITEM_RANGE;
    /** A container only rattles if no player is this close (so nobody is about to open it). */
    public static final double CONTAINER_PLAYER_CLEARANCE = Balance.Haunting.CONTAINER_PLAYER_CLEARANCE;

    private final Map<HauntingAction, List<BlockPos>> blocks = new EnumMap<>(HauntingAction.class);
    final List<HangingEntity> decorations = new ArrayList<>();
    final List<ItemEntity> items = new ArrayList<>();
    final List<Player> walkers = new ArrayList<>();

    private HauntingTargets() {
    }

    public static HauntingTargets find(ServerLevel level, GhostEntity ghost) {

        HauntingTargets targets = new HauntingTargets();
        BlockPos centre = ghost.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-RANGE, -RANGE, -RANGE), centre.offset(RANGE, RANGE, RANGE))) {
            if (pos.distSqr(centre) > RANGE * RANGE || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            HauntingAction action = blockAction(level, pos, state);
            if (action != null) {
                targets.blocks.computeIfAbsent(action, key -> new ArrayList<>()).add(pos.immutable());
            }
        }
        AABB box = ghost.getBoundingBox().inflate(RANGE);
        for (HangingEntity hanging : level.getEntitiesOfClass(HangingEntity.class, box, entity -> entity.isAlive() && entity.distanceToSqr(ghost) <= RANGE * RANGE)) {
            if (hanging instanceof Painting || hanging instanceof ItemFrame) {
                targets.decorations.add(hanging);
            }
        }
        targets.items.addAll(level.getEntitiesOfClass(ItemEntity.class, ghost.getBoundingBox().inflate(ITEM_RANGE),
                item -> item.isAlive() && item.distanceToSqr(ghost) <= ITEM_RANGE * ITEM_RANGE));
        targets.walkers.addAll(level.getEntitiesOfClass(Player.class, box,
                player -> player.isAlive() && !player.isSpectator() && player.onGround() && player.distanceToSqr(ghost) <= RANGE * RANGE));
        return targets;
    }

    /**
     * Which block action, if any, this block is a target for. Package-visible for tests.
     */
    static HauntingAction blockAction(ServerLevel level, BlockPos pos, BlockState state) {

        if (state.is(ModTags.SNUFFABLE_TORCHES)) {
            return HauntingAction.SNUFF_LIGHT;
        }
        if (state.is(ModTags.SNUFFABLE_FLAMES) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            return HauntingAction.SNUFF_LIGHT;
        }
        if (state.is(Blocks.LEVER)) {
            return HauntingAction.FLIP_LEVER;
        }
        if (state.is(ModTags.HAUNTED_OPENINGS)) {
            // A door is two blocks; count it once.
            boolean upperDoorHalf = state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
            return upperDoorHalf ? null : HauntingAction.TOGGLE_OPENING;
        }
        if (state.is(ModTags.PHANTOM_CONTAINERS)) {
            Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, CONTAINER_PLAYER_CLEARANCE, false);
            return nearest == null ? HauntingAction.PHANTOM_CHEST : null;
        }
        if (isWallFace(level, pos, state)) {
            return HauntingAction.KNOCKING;
        }
        return null;
    }

    /**
     * A full solid block with open air beside it: something to knock on.
     */
    private static boolean isWallFace(ServerLevel level, BlockPos pos, BlockState state) {

        // Part of a wall at least two blocks high (so a floor edge doesn't count), with open air on one side.
        if (!state.isCollisionShapeFullBlock(level, pos) || !level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above())) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }

    public boolean has(HauntingAction action) {

        return switch (action) {
            case KNOCK_OFF_DECORATION -> !this.decorations.isEmpty();
            case TOSS_ITEMS -> !this.items.isEmpty();
            case FOOTSTEPS -> !this.walkers.isEmpty();
            default -> !blocks(action).isEmpty();
        };
    }

    public List<BlockPos> blocks(HauntingAction action) {

        return this.blocks.getOrDefault(action, List.of());
    }
}
