package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.client.EmfLevelProperty;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.tool.EmfLevels;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ModModelProvider extends ModelProvider {

    public ModModelProvider(PackOutput output) {

        super(output, Constants.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        itemModels.generateFlatItem(ModItems.SPECTRAL_GOGGLES.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LINGERING_SPIRIT_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SPIRIT_THERMOMETER.get(), ModelTemplates.FLAT_ITEM);
        generateEmfReader(itemModels);
        blockModels.createAirLikeBlock(ModBlocks.GHOST_MARKER.get(), Items.SOUL_LANTERN);
    }

    /**
     * One flat model per level (emf_reader_1 to emf_reader_5), picked by the {@code paranormalcraft:emf_level}
     * range property.
     */
    private static void generateEmfReader(ItemModelGenerators itemModels) {

        Item reader = ModItems.EMF_READER.get();
        List<RangeSelectItemModel.Entry> levels = new ArrayList<>();
        for (int level = EmfLevels.MIN_LEVEL + 1; level <= EmfLevels.MAX_LEVEL; level++) {
            levels.add(ItemModelUtils.override(ItemModelUtils.plainModel(itemModels.createFlatItemModel(reader, "_" + level, ModelTemplates.FLAT_ITEM)), level));
        }
        var baseline = ItemModelUtils.plainModel(itemModels.createFlatItemModel(reader, "_" + EmfLevels.MIN_LEVEL, ModelTemplates.FLAT_ITEM));
        itemModels.itemModelOutput.accept(reader, ItemModelUtils.rangeSelect(new EmfLevelProperty(), baseline, levels));
    }
}
