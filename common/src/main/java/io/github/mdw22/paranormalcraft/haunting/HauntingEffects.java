package io.github.mdw22.paranormalcraft.haunting;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What each haunting action actually does. Every effect is a real, shared world event that all nearby players see and
 * hear.
 */
final class HauntingEffects {

    /** The chest lid stays up this long (about 1.5 s). */
    static final int CHEST_OPEN_TICKS = Balance.Haunting.CHEST_OPEN_TICKS;
    /** Footsteps are spread over about 2 s. */
    static final int FOOTSTEP_SPAN_TICKS = Balance.Haunting.FOOTSTEP_SPAN_TICKS;
    static final int KNOCK_GAP_TICKS = Balance.Haunting.KNOCK_GAP_TICKS;

    private HauntingEffects() {
    }

    /**
     * @return a short description of what happened (for logs and debug output), or null if nothing could be done
     */
    static String perform(HauntingAction action, HauntingTargets targets, ServerLevel level, GhostEntity ghost) {

        RandomSource random = ghost.getRandom();
        return switch (action) {
            case KNOCK_OFF_DECORATION -> knockOffDecoration(pickOne(targets.decorations, random), level, ghost);
            case SNUFF_LIGHT -> snuffLight(pickOne(targets.blocks(action), random), level, ghost);
            case FLIP_LEVER -> flipLever(pickOne(targets.blocks(action), random), level);
            case TOGGLE_OPENING -> toggleOpening(pickOne(targets.blocks(action), random), level, ghost);
            case PHANTOM_CHEST -> phantomChest(pickOne(targets.blocks(action), random), level);
            case TOSS_ITEMS -> tossItems(targets.items, level, random);
            case FOOTSTEPS -> footsteps(pickOne(targets.walkers, random), level, random);
            case KNOCKING -> knocking(pickOne(targets.blocks(action), random), level, random);
        };
    }

    private static <T> T pickOne(List<T> options, RandomSource random) {

        return options.get(random.nextInt(options.size()));
    }

    private static String describe(BlockPos pos, BlockState state) {

        return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " at " + pos.toShortString();
    }

    /**
     * Breaks a painting, or an item frame and its contents, through vanilla's own damage path (so drops, sounds and the
     * mobGriefing check all match vanilla). An item frame first drops its item, then itself.
     */
    private static String knockOffDecoration(HangingEntity decoration, ServerLevel level, GhostEntity ghost) {

        String what = BuiltInRegistries.ENTITY_TYPE.getKey(decoration.getType()) + " at " + decoration.blockPosition().toShortString();
        for (int hit = 0; hit < 3 && !decoration.isRemoved(); hit++) {
            decoration.hurtServer(level, level.damageSources().mobAttack(ghost), 1.0F);
        }
        return decoration.isRemoved() ? what : null;
    }

    private static String snuffLight(BlockPos pos, ServerLevel level, GhostEntity ghost) {

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof AbstractCandleBlock) {
            AbstractCandleBlock.extinguish(null, state, level, pos);
            smoke(level, pos);
        } else if (block instanceof CampfireBlock) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL_IMMEDIATE);
            CampfireBlock.dowse(ghost, level, pos, state);
            level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
            smoke(level, pos);
        } else {
            // A torch pops off and drops as an item.
            level.destroyBlock(pos, true, ghost);
        }
        return describe(pos, state);
    }

    private static void smoke(ServerLevel level, BlockPos pos) {

        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.01);
    }

    private static String flipLever(BlockPos pos, ServerLevel level) {

        BlockState state = level.getBlockState(pos);
        ((LeverBlock) state.getBlock()).pull(state, level, pos, null);
        return describe(pos, state);
    }

    /**
     * Swings a wooden door, trapdoor or fence gate with its own sound. Iron variants are never in the target tag.
     */
    private static String toggleOpening(BlockPos pos, ServerLevel level, GhostEntity ghost) {

        BlockState state = level.getBlockState(pos);
        boolean opening = !state.getValue(BlockStateProperties.OPEN);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(ghost, level, state, pos, opening);
            return describe(pos, state);
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, opening), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (state.getBlock() instanceof TrapDoorBlock) {
            BlockSetType type = blockSetType(name.replace("_trapdoor", ""));
            level.playSound(null, pos, opening ? type.trapdoorOpen() : type.trapdoorClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        } else if (state.getBlock() instanceof FenceGateBlock) {
            WoodType type = woodType(name.replace("_fence_gate", ""));
            level.playSound(null, pos, opening ? type.fenceGateOpen() : type.fenceGateClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        }
        level.gameEvent(ghost, opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return describe(pos, state);
    }

    // Vanilla keeps a trapdoor's and gate's sound set private; their ids are named after it ("spruce_trapdoor").
    private static BlockSetType blockSetType(String name) {

        return BlockSetType.values().filter(type -> type.name().equals(name)).findFirst().orElse(BlockSetType.OAK);
    }

    private static WoodType woodType(String name) {

        return WoodType.values().filter(type -> type.name().equals(name)).findFirst().orElse(WoodType.OAK);
    }

    /**
     * The lid lifts and drops again about 1.5 s later. The inventory is never opened or changed.
     */
    private static String phantomChest(BlockPos pos, ServerLevel level) {

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof ChestBlock) {
            List<BlockPos> halves = state.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                    ? List.of(pos)
                    : List.of(pos, pos.relative(ChestBlock.getConnectedDirection(state)));
            halves.forEach(half -> level.blockEvent(half, block, 1, 1));
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            HauntingScheduler.schedule(level, CHEST_OPEN_TICKS, () -> {
                if (level.getBlockState(pos).is(block)) {
                    halves.forEach(half -> level.blockEvent(half, block, 1, 0));
                    level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
                }
            });
        } else if (block instanceof BarrelBlock) {
            level.setBlock(pos, state.setValue(BarrelBlock.OPEN, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            HauntingScheduler.schedule(level, CHEST_OPEN_TICKS, () -> {
                BlockState now = level.getBlockState(pos);
                if (now.is(block)) {
                    level.setBlock(pos, now.setValue(BarrelBlock.OPEN, false), Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
                }
            });
        }
        return describe(pos, state);
    }

    private static String tossItems(List<ItemEntity> items, ServerLevel level, RandomSource random) {

        int count = Math.min(items.size(), 1 + random.nextInt(3));
        for (int i = 0; i < count; i++) {
            ItemEntity item = items.get(random.nextInt(items.size()));
            item.setDeltaMovement(item.getDeltaMovement().add((random.nextDouble() - 0.5) * 0.4, 0.2 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.4));
            item.hurtMarked = true;
        }
        return count + " item(s)";
    }

    /**
     * 3 to 5 steps on the sound of the block under the player, 3 blocks behind them, over about 2 s.
     */
    private static String footsteps(Player player, ServerLevel level, RandomSource random) {

        BlockState ground = level.getBlockState(player.getOnPos());
        SoundType sound = ground.getSoundType();
        int steps = 3 + random.nextInt(3);
        for (int step = 0; step < steps; step++) {
            HauntingScheduler.schedule(level, (long) step * FOOTSTEP_SPAN_TICKS / steps, () -> {
                if (!player.isRemoved() && player.level() == level) {
                    Vec3 behind = player.position().subtract(player.getLookAngle().multiply(1, 0, 1).normalize().scale(3.0));
                    level.playSound(null, behind.x, behind.y, behind.z, sound.getStepSound(), SoundSource.HOSTILE, sound.getVolume() * 0.5F, sound.getPitch());
                }
            });
        }
        return steps + " step(s) behind " + player.getGameProfile().name();
    }

    private static String knocking(BlockPos pos, ServerLevel level, RandomSource random) {

        int knocks = 2 + random.nextInt(2);
        for (int knock = 0; knock < knocks; knock++) {
            HauntingScheduler.schedule(level, (long) knock * KNOCK_GAP_TICKS, () ->
                    level.playSound(null, pos, SoundType.WOOD.getHitSound(), SoundSource.HOSTILE, 1.0F, 0.7F + level.getRandom().nextFloat() * 0.2F));
        }
        return knocks + " knock(s) on " + describe(pos, level.getBlockState(pos));
    }
}
