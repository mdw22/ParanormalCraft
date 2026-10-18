package io.github.mdw22.paranormalcraft.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Fills a creative tab. Stands in for vanilla's display items generator, whose {@code Output} type is not visible
 * from common code.
 */
@FunctionalInterface
public interface TabContents {

    void addItems(CreativeModeTab.ItemDisplayParameters parameters, Consumer<ItemStack> output);
}
