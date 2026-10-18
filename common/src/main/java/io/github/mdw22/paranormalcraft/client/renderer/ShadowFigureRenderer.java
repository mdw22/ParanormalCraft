package io.github.mdw22.paranormalcraft.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import io.github.mdw22.paranormalcraft.entity.ShadowFigureEntity;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * An almost-black silhouette at about 85% opacity, fading out when its entity fades. Always drawn, even though the
 * entity reports itself invisible (which only serves to hide its hitbox and shadow).
 */
public class ShadowFigureRenderer extends GeoEntityRenderer<ShadowFigureEntity, EntityRenderState> {

    private static final float OPACITY = 0.85F;
    private static final int COLOUR = 0x0B0B10;

    public ShadowFigureRenderer(EntityRendererProvider.Context context) {

        super(context, ModEntities.SHADOW_FIGURE.get());
        this.shadowRadius = 0.0F;
    }

    @Override
    public int getRenderColor(ShadowFigureEntity animatable, @Nullable Void relatedObject, float partialTick) {

        return ARGB.color(OPACITY * animatable.fade(partialTick), COLOUR);
    }

    @Override
    public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {

        return RenderTypes.entityTranslucent(texture);
    }
}
