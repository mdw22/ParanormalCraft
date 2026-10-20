package io.github.mdw22.paranormalcraft.phase;

import io.github.mdw22.paranormalcraft.Balance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Spectral Step charm: use it to be phased for 5 s ({@link Phasing}). One use of durability and a 60 s
 * cooldown (vanilla's item cooldown overlay is the HUD cue). Repairable with Lingering Ectoplasm.
 */
public class SpectralStepItem extends Item {

    public static final int COOLDOWN_TICKS = Balance.Phase.COOLDOWN_TICKS;

    public SpectralStepItem(Item.Properties properties) {

        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (Phasing.state(serverPlayer) != null) {
                return InteractionResult.FAIL;
            }
            Phasing.start(serverPlayer);
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            stack.hurtAndBreak(1, serverPlayer, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
