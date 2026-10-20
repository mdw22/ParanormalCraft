package io.github.mdw22.paranormalcraft.debug;

import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * {@code /paranormalcraft debug kit}: the test kit in the hotbar. Later milestones add their tools to {@link #TOOLS}.
 */
public final class DebugKit {

    /** Hotbar slots 1 to 9, then the first main-inventory slots, in order. */
    public static final List<Supplier<ItemStack>> TOOLS = List.of(
            () -> new ItemStack(ModItems.SPECTRAL_GOGGLES.get()),
            () -> new ItemStack(ModItems.EMF_READER.get()),
            () -> new ItemStack(ModItems.SPIRIT_THERMOMETER.get()),
            () -> new ItemStack(Items.NETHERITE_SWORD),
            () -> new ItemStack(ModItems.TETHER_WAND.get()),
            () -> new ItemStack(ModItems.GHOST_TRAP.get(), 3),
            () -> new ItemStack(ModItems.WARD_LANTERN.get(), 2),
            () -> new ItemStack(ModItems.FIELD_JOURNAL.get()),
            () -> new ItemStack(ModItems.CONTAINMENT_UNIT.get()),
            () -> new ItemStack(Items.SOUL_SAND, 4),
            () -> new ItemStack(ModItems.SPIRIT_VESSEL.get()),
            () -> new ItemStack(ModItems.SPECTRAL_STEP.get()));
    /** The hotbar slot the Tether Wand goes in. */
    public static final int WAND_SLOT = 4;

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
            select(player, 0);
        }
        player.inventoryMenu.broadcastChanges();
    }

    /**
     * Selects a hotbar slot on the server and tells the client, which otherwise keeps its own selection.
     */
    public static void select(ServerPlayer player, int slot) {

        player.getInventory().setSelectedSlot(slot);
        player.connection.send(new ClientboundSetHeldSlotPacket(slot));
    }
}
