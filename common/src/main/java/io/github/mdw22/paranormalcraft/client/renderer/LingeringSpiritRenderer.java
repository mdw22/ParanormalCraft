package io.github.mdw22.paranormalcraft.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Draws the spirit translucent (about 60% opacity, see {@link GhostAlpha}), tinted soul-cyan for goggle wearers.
 * Without goggles a manifest fades in over 6 ticks and out over 10. When the local player can't see it and it isn't
 * fading out, the entity reports itself invisible on the client, so nothing is drawn: no model, shadow, name tag or
 * hitbox. A Glowing ghost still gets vanilla's outline.
 */
public class LingeringSpiritRenderer extends GeoEntityRenderer<LingeringSpiritEntity, LivingEntityRenderState> {

    private static final int PLAIN_TINT = 0xFFFFFF;
    private static final int GOGGLES_TINT = 0x9CF2F2;
    /** Field Journal dummies: a near-black shape before the player has seen the type, a firmer portrait after. */
    private static final int SILHOUETTE = 0xE8101014;
    private static final float PORTRAIT_ALPHA = 0.85F;

    public LingeringSpiritRenderer(EntityRendererProvider.Context context) {

        super(context, ModEntities.LINGERING_SPIRIT.get());
        this.shadowRadius = 0.35F;
    }

    @Override
    public void addRenderData(LingeringSpiritEntity animatable, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {

        // GeckoLib adds GeoRenderState to vanilla render states at runtime; common code can't see that, hence the cast.
        ((GeoRenderState) renderState).addGeckolibData(LingeringSpiritEntity.MANIFESTING, animatable.isManifesting());
    }

    @Override
    public int getRenderColor(LingeringSpiritEntity animatable, @Nullable Void relatedObject, float partialTick) {

        if (animatable.portrait() == GhostEntity.PORTRAIT_SILHOUETTE) {
            return SILHOUETTE;
        }
        if (animatable.portrait() == GhostEntity.PORTRAIT_SEEN) {
            return ARGB.color(PORTRAIT_ALPHA, GOGGLES_TINT);
        }
        Player viewer = GhostVisibility.clientViewer();
        boolean goggles = viewer != null && GhostVisibility.wearsGoggles(viewer);
        return ARGB.color(GhostAlpha.alpha(animatable, viewer, partialTick), goggles ? GOGGLES_TINT : PLAIN_TINT);
    }

    @Override
    public @Nullable RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {

        if (renderState.isInvisible) {
            return renderState.appearsGlowing() ? RenderTypes.outline(texture) : null;
        }
        return RenderTypes.entityTranslucent(texture);
    }
}
