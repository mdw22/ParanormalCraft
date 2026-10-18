package io.github.mdw22.paranormalcraft.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Draws the spirit translucent (about 60% opacity), tinted soul-cyan for goggle wearers. When the local player can't
 * see it, the entity reports itself invisible on the client, so nothing is drawn: no model, shadow, name tag or
 * hitbox. A Glowing ghost still gets vanilla's outline.
 */
public class LingeringSpiritRenderer extends GeoEntityRenderer<LingeringSpiritEntity, LivingEntityRenderState> {

    private static final float OPACITY = 0.6F;
    private static final int PLAIN_TINT = 0xFFFFFF;
    private static final int GOGGLES_TINT = 0x9CF2F2;
    /** The manifest fade-in, in ticks. */
    private static final float FADE_IN_TICKS = 6.0F;

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

        Player viewer = GhostVisibility.clientViewer();
        boolean goggles = viewer != null && GhostVisibility.wearsGoggles(viewer);
        boolean seesAnyway = viewer != null && (goggles || RevealDebug.isRevealed(viewer) || viewer.isSpectator());
        float alpha = OPACITY;
        float manifestAge = animatable.manifestAge(partialTick);
        if (!seesAnyway && manifestAge >= 0.0F) {
            alpha *= Mth.clamp(manifestAge / FADE_IN_TICKS, 0.0F, 1.0F);
        }
        return ARGB.color(alpha, goggles ? GOGGLES_TINT : PLAIN_TINT);
    }

    @Override
    public @Nullable RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {

        if (renderState.isInvisible) {
            return renderState.appearsGlowing() ? RenderTypes.outline(texture) : null;
        }
        return RenderTypes.entityTranslucent(texture);
    }
}
