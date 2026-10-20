package io.github.mdw22.paranormalcraft.mixin;

import io.github.mdw22.paranormalcraft.phase.Phasing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Spectral Step: a phased player's block collisions skip {@code #paranormalcraft:phase_passable}. Every
 * entity-aware collision query (movement, the client's prediction and the server's move check) asks the entity's
 * collision context for each block's shape, so this one hook covers both sides. Context-free queries (suffocation,
 * the end-of-phase check) still see the real shape. Same on both loaders.
 */
@Mixin(EntityCollisionContext.class)
public abstract class EntityCollisionContextMixin {

    @Shadow
    public abstract @Nullable Entity getEntity();

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void paranormalcraft$phase(BlockState state, CollisionGetter collisionGetter, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {

        if (!Phasing.nobodyPhased() && Phasing.passes(getEntity(), state)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
