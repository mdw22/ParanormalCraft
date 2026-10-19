package io.github.mdw22.paranormalcraft.entity;

import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown Ghost Trap. Landing on top of a solid block arms a trap there; anything else (a wall, a mob, no room) drops
 * it as an item.
 */
public class ThrownGhostTrap extends ThrowableItemProjectile {

    public ThrownGhostTrap(EntityType<? extends ThrownGhostTrap> type, Level level) {

        super(type, level);
    }

    public ThrownGhostTrap(Level level, LivingEntity owner, ItemStack stack) {

        super(ModEntities.THROWN_GHOST_TRAP.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {

        return ModItems.GHOST_TRAP.get();
    }

    @Override
    protected double getDefaultGravity() {

        return 0.05;
    }

    @Override
    protected void onHit(HitResult result) {

        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        boolean armed = false;
        if (result instanceof BlockHitResult blockHit && blockHit.getDirection() == Direction.UP) {
            BlockPos pos = blockHit.getBlockPos().above();
            BlockState trap = ModBlocks.GHOST_TRAP.get().defaultBlockState();
            if (level.getBlockState(pos).canBeReplaced() && trap.canSurvive(level, pos)) {
                level.setBlock(pos, trap, Block.UPDATE_ALL);
                armed = true;
            }
        }
        if (!armed) {
            this.spawnAtLocation(level, new ItemStack(ModItems.GHOST_TRAP.get()));
        }
        this.discard();
    }
}
