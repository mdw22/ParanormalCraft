package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The one rule for who can see a ghost, used by the client renderer and picking and by server damage checks.
 */
public final class GhostVisibility {

    // Set by client init so entity code can ask about the local player without referencing client classes.
    private static Supplier<@Nullable Player> clientViewer = () -> null;

    private GhostVisibility() {
    }

    public static boolean canSee(Player viewer, GhostEntity ghost) {

        return wearsGoggles(viewer)
                || ghost.isManifesting()
                || RevealDebug.isRevealed(viewer)
                || viewer.isSpectator();
    }

    public static boolean wearsGoggles(Player player) {

        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SPECTRAL_GOGGLES.get());
    }

    /**
     * Client side: whether the local player can see this ghost. False when there is no local player.
     */
    public static boolean canClientSee(GhostEntity ghost) {

        Player viewer = clientViewer.get();
        return viewer != null && canSee(viewer, ghost);
    }

    /**
     * Client side: the local player, or null on a dedicated server or before joining a world.
     */
    public static @Nullable Player clientViewer() {

        return clientViewer.get();
    }

    /**
     * Called once from client init.
     */
    public static void setClientViewer(Supplier<@Nullable Player> viewer) {

        clientViewer = viewer;
    }
}
