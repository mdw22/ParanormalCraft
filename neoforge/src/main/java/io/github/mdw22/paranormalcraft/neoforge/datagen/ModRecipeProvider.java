package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * Tool recipes. Each unlocks in the recipe book once the player has a copper ingot.
 */
public class ModRecipeProvider extends RecipeProvider {

    private static final String HAS_COPPER = "has_copper_ingot";

    protected ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {

        super(registries, output);
    }

    @Override
    protected void buildRecipes() {

        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, ModItems.SPECTRAL_GOGGLES.get())
                .pattern("LCL")
                .pattern("TAT")
                .define('L', Items.LEATHER)
                .define('C', Items.COPPER_INGOT)
                .define('T', Items.TINTED_GLASS)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy(HAS_COPPER, has(Items.COPPER_INGOT))
                .save(this.output);
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, ModItems.EMF_READER.get())
                .pattern("CTC")
                .pattern("CRC")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('T', Items.REDSTONE_TORCH)
                .define('R', Items.REDSTONE)
                .unlockedBy(HAS_COPPER, has(Items.COPPER_INGOT))
                .save(this.output);
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, ModItems.SPIRIT_THERMOMETER.get())
                .pattern("G")
                .pattern("R")
                .pattern("C")
                .define('G', Items.GLASS_PANE)
                .define('R', Items.REDSTONE)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(HAS_COPPER, has(Items.COPPER_INGOT))
                .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {

            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {

            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {

            return "Paranormal Craft recipes";
        }
    }
}
