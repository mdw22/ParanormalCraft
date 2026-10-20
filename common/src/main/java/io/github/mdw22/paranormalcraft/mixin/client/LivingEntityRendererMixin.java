package io.github.mdw22.paranormalcraft.mixin.client;

import io.github.mdw22.paranormalcraft.phase.Phasing;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player phased by the Spectral Step is drawn translucent for everyone: the render state is marked invisible but
 * not invisible to the viewer, which is vanilla's own see-through look (as for an invisible teammate).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void paranormalcraft$phasedTranslucent(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {

        if (entity instanceof Player && Phasing.isPhased(entity)) {
            state.isInvisible = true;
            state.isInvisibleToPlayer = false;
        }
    }
}
