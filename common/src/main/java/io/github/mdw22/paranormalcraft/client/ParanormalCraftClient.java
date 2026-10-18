package io.github.mdw22.paranormalcraft.client;

import io.github.mdw22.paranormalcraft.client.renderer.LingeringSpiritRenderer;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.client.Minecraft;

/**
 * Client-only setup, called from each loader's client entrypoint. Nothing on a server code path may reference this
 * package.
 */
public final class ParanormalCraftClient {

    private ParanormalCraftClient() {
    }

    public static void init() {

        ModConfigs.loadClient();
        GhostVisibility.setClientViewer(() -> Minecraft.getInstance().player);
    }

    public static void registerEntityRenderers(EntityRendererRegistrar registrar) {

        registrar.register(ModEntities.LINGERING_SPIRIT.get(), LingeringSpiritRenderer::new);
    }
}
