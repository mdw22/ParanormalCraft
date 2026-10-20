package io.github.mdw22.paranormalcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Spectral Residue: a rare leftover of processing a ghost. It does nothing yet; later recipes will use it.
 */
public class SpectralResidueItem extends Item {

    public SpectralResidueItem(Item.Properties properties) {

        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {

        builder.accept(Component.translatable("item.paranormalcraft.spectral_residue.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
