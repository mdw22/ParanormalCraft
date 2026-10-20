package io.github.mdw22.paranormalcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.familiar.WispFamiliarEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * The Wisp Familiar: a full-bright, camera-facing glow (a soft halo round a bright core), pale soul-cyan, red-tinted
 * while warning, pulsing larger while guiding. Visible to everyone, goggles or not.
 */
public class WispFamiliarRenderer extends EntityRenderer<WispFamiliarEntity, WispFamiliarRenderer.State> {

    private static final Identifier TEXTURE = Constants.id("textures/entity/wisp_familiar.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);
    private static final int PALE = 0xC8F6FA;
    private static final int WARN = 0xFF5A48;
    /** The Lingering Spirit is about 1.3 blocks tall; the wisp is {@code SCALE} of that. */
    private static final float SIZE = 1.3F * Balance.Familiar.SCALE;

    /** What the renderer needs from the wisp. */
    public static class State extends EntityRenderState {

        boolean warning;
        boolean guiding;
    }

    public WispFamiliarRenderer(EntityRendererProvider.Context context) {

        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {

        return new State();
    }

    @Override
    public void extractRenderState(WispFamiliarEntity entity, State state, float partialTicks) {

        super.extractRenderState(entity, state, partialTicks);
        state.warning = entity.isWarning();
        state.guiding = entity.isGuiding();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.2F, 0.0F);
        poseStack.mulPose(camera.orientation);
        float pulse = 1.0F + 0.08F * Mth.sin(state.ageInTicks * 0.25F) + (state.guiding ? 0.25F + 0.15F * Mth.sin(state.ageInTicks * 0.5F) : 0.0F);
        int rgb = state.warning ? WARN : PALE;
        quad(poseStack, collector, SIZE * 2.4F * pulse, rgb, 150);
        quad(poseStack, collector, SIZE * 1.0F * pulse, state.warning ? 0xFFD0C8 : 0xFFFFFF, 255);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void quad(PoseStack poseStack, SubmitNodeCollector collector, float size, int rgb, int alpha) {

        poseStack.pushPose();
        poseStack.scale(size, size, size);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.5F, r, g, b, alpha, 0.0F, 1.0F);
            vertex(buffer, pose, 0.5F, -0.5F, r, g, b, alpha, 1.0F, 1.0F);
            vertex(buffer, pose, 0.5F, 0.5F, r, g, b, alpha, 1.0F, 0.0F);
            vertex(buffer, pose, -0.5F, 0.5F, r, g, b, alpha, 0.0F, 0.0F);
        });
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int r, int g, int b, int a, float u, float v) {

        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
