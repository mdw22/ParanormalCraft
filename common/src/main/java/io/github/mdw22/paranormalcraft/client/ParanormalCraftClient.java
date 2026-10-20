package io.github.mdw22.paranormalcraft.client;

import io.github.mdw22.paranormalcraft.client.containment.ContainmentUnitScreen;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.client.hallucination.ClientHallucinations;
import io.github.mdw22.paranormalcraft.client.renderer.LingeringSpiritRenderer;
import io.github.mdw22.paranormalcraft.client.renderer.ShadowFigureRenderer;
import io.github.mdw22.paranormalcraft.client.renderer.WispFamiliarRenderer;
import io.github.mdw22.paranormalcraft.client.tether.ClientBeams;
import io.github.mdw22.paranormalcraft.client.ward.ClientWardPreview;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.trace.ClientTraceHook;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.phase.Phasing;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

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
        ClientTraceHook.install(ClientTrace::event, ClientTrace::markNextSoundLocal);
        ClientServices.PLATFORM.registerRangeSelectProperty(EmfLevelProperty.ID, EmfLevelProperty.MAP_CODEC);
        ClientServices.PLATFORM.registerMenuScreen(ModMenus.CONTAINMENT_UNIT, ContainmentUnitScreen::new);
    }

    /**
     * Called at the end of every client tick.
     */
    public static void tick(Minecraft minecraft) {

        ClientTrace.tick(minecraft);
        if (minecraft.level == null) {
            ClientToolState.reset();
            ClientHallucinations.reset();
            ClientTrace.reset();
            ClientBeams.reset();
            ClientWardPreview.reset();
            Phasing.resetClient();
            return;
        }
        ClientToolState.tick(minecraft);
        ClientHallucinations.tick(minecraft);
        ClientBeams.tick(minecraft);
        ClientWardPreview.tick(minecraft);
    }

    public static void registerEntityRenderers(EntityRendererRegistrar registrar) {

        registrar.register(ModEntities.LINGERING_SPIRIT.get(), LingeringSpiritRenderer::new);
        registrar.register(ModEntities.SHADOW_FIGURE.get(), ShadowFigureRenderer::new);
        registrar.register(ModEntities.THROWN_GHOST_TRAP.get(), ThrownItemRenderer::new);
        registrar.register(ModEntities.WISP_FAMILIAR.get(), WispFamiliarRenderer::new);
    }
}
