package io.github.mdw22.paranormalcraft.debug;

import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * {@code /paranormalcraft debug kit}: the test kit in the hotbar. Later milestones add their tools to {@link #TOOLS}.
 */
public final class DebugKit {

    /** Hotbar slots 1 to 4, in order. */
    public static final List<Supplier<ItemStack>> TOOLS = List.of(
            () -> new ItemStack(ModItems.SPECTRAL_GOGGLES.get()),
            () -> new ItemStack(ModItems.EMF_READER.get()),
            () -> new ItemStack(ModItems.SPIRIT_THERMOMETER.get()),
            () -> new ItemStack(Items.NETHERITE_SWORD));

    private DebugKit() {
    }

    /**
     * Clears the inventory (including armour), then puts the tools in the hotbar unless {@code tools} is false.
     */
    public static void give(ServerPlayer player, boolean tools) {

        player.getInventory().clearContent();
        if (tools) {
            for (int slot = 0; slot < TOOLS.size(); slot++) {
                player.getInventory().setItem(slot, TOOLS.get(slot).get());
            }
            player.getInventory().setSelectedSlot(0);
        }
        player.inventoryMenu.broadcastChanges();
    }
}
