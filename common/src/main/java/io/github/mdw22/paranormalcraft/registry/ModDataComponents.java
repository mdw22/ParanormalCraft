package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.WandHeat;
import net.minecraft.core.component.DataComponentType;

/**
 * Item data components: the Tether Wand's heat ({@code wand_heat}) and a Filled Ghost Trap's ghost
 * ({@code captured_ghost}).
 */
public final class ModDataComponents {

    /** The Tether Wand's heat, overheat lockout and beam-time count; synced so the item bar can show heat. */
    public static final RegistryEntry<DataComponentType<WandHeat>> WAND_HEAT = RegistrationHelper.dataComponent("wand_heat",
            // Heat changes every tick while firing: without ignoreSwapAnimation the held wand would keep replaying
            // vanilla's re-equip animation.
            builder -> builder.persistent(WandHeat.CODEC).networkSynchronized(WandHeat.STREAM_CODEC).ignoreSwapAnimation());
    /** The ghost inside a Filled Ghost Trap. */
    public static final RegistryEntry<DataComponentType<CapturedGhost>> CAPTURED_GHOST = RegistrationHelper.dataComponent("captured_ghost",
            builder -> builder.persistent(CapturedGhost.CODEC).networkSynchronized(CapturedGhost.STREAM_CODEC));

    private ModDataComponents() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
