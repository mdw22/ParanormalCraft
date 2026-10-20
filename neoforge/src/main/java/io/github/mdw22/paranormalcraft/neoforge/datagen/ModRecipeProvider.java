package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * Tool recipes. The Milestone 1 tools unlock in the recipe book with a copper ingot; the Tether Wand with a lightning
 * rod or an amethyst shard; the Ghost Trap with tinted glass or a Tether Wand.
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
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, ModItems.TETHER_WAND.get())
                .pattern(" A ")
                .pattern(" L ")
                .pattern("CRC")
                .define('A', Items.AMETHYST_SHARD)
                .define('L', Items.LIGHTNING_ROD.weathering().unaffected())
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_lightning_rod", has(Items.LIGHTNING_ROD.weathering().unaffected()))
                .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
                .save(this.output);
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, ModItems.GHOST_TRAP.get())
                .pattern("ITI")
                .pattern("CRC")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('T', Items.TINTED_GLASS)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', Items.STONE_PRESSURE_PLATE)
                .unlockedBy("has_tinted_glass", has(Items.TINTED_GLASS))
                .unlockedBy("has_tether_wand", has(ModItems.TETHER_WAND.get()))
                .save(this.output);
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.MISC, ModItems.CONTAINMENT_UNIT.get())
                .pattern("IGI")
                .pattern("CTC")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.TINTED_GLASS)
                .define('C', Items.COPPER_BLOCK.weathering().unaffected())
                .define('T', ModItems.GHOST_TRAP.get())
                .define('R', Items.REDSTONE_BLOCK)
                .unlockedBy("has_ghost_trap", has(ModItems.GHOST_TRAP.get()))
                .save(this.output);
        ShapelessRecipeBuilder.shapeless(this.items, RecipeCategory.DECORATIONS, ModItems.WARD_LANTERN.get())
                .requires(Items.LANTERN)
                .requires(ModItems.LINGERING_ECTOPLASM.get(), 4)
                .requires(Items.AMETHYST_SHARD)
                .unlockedBy("has_lingering_ectoplasm", has(ModItems.LINGERING_ECTOPLASM.get()))
                .save(this.output);
        ShapelessRecipeBuilder.shapeless(this.items, RecipeCategory.MISC, ModItems.FIELD_JOURNAL.get())
                .requires(Items.BOOK)
                .requires(Items.COPPER_INGOT)
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
