package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlock;
import io.github.mdw22.paranormalcraft.client.EmfLevelProperty;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlock;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.tool.EmfLevels;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Generates item models, item definitions and blockstates. The trap blocks point at hand-written cuboid models in
 * {@code common/src/main/resources/assets/paranormalcraft/models/block}.
 */
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
        itemModels.generateFlatItem(ModItems.TETHER_WAND.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.GHOST_TRAP.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FILLED_GHOST_TRAP.get(), ModelTemplates.FLAT_ITEM);
        // The trap blocks use hand-written cuboid models (assets/paranormalcraft/models/block/*ghost_trap*.json).
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.GHOST_TRAP.get())
                .with(PropertyDispatch.initial(GhostTrapBlock.OPEN)
                        .select(false, BlockModelGenerators.plainVariant(Constants.id("block/ghost_trap")))
                        .select(true, BlockModelGenerators.plainVariant(Constants.id("block/ghost_trap_open")))));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.FILLED_GHOST_TRAP.get(),
                BlockModelGenerators.plainVariant(Constants.id("block/filled_ghost_trap"))));
        generateContainmentUnit(blockModels);
        // Placeholder: vanilla's lantern shapes with the soul lantern texture; its own flat item icon.
        Block ward = ModBlocks.WARD_LANTERN.get();
        MultiVariant standing = BlockModelGenerators.plainVariant(TexturedModel.LANTERN.get(Blocks.SOUL_LANTERN).create(ward, blockModels.modelOutput));
        MultiVariant hanging = BlockModelGenerators.plainVariant(TexturedModel.HANGING_LANTERN.get(Blocks.SOUL_LANTERN).create(ward, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ward)
                .with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.HANGING, hanging, standing)));
        itemModels.generateFlatItem(ModItems.WARD_LANTERN.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LINGERING_ECTOPLASM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SPECTRAL_RESIDUE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FIELD_JOURNAL.get(), ModelTemplates.FLAT_ITEM);
    }

    /**
     * An orientable cabinet: a dark front when empty, a cyan-lit front with ghosts inside, a red front while Unstable.
     */
    private static void generateContainmentUnit(BlockModelGenerators blockModels) {

        Block unit = ModBlocks.CONTAINMENT_UNIT.get();
        Material side = new Material(Constants.id("block/containment_unit_side"));
        Material top = new Material(Constants.id("block/containment_unit_top"));
        MultiVariant empty = model(blockModels, unit, "", side, top, "block/containment_unit_front");
        MultiVariant lit = model(blockModels, unit, "_lit", side, top, "block/containment_unit_front_lit");
        MultiVariant unstable = model(blockModels, unit, "_unstable", side, top, "block/containment_unit_front_unstable");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(unit)
                .with(PropertyDispatch.initial(ContainmentUnitBlock.GHOSTS, ContainmentUnitBlock.UNSTABLE)
                        .generate((ghosts, isUnstable) -> isUnstable ? unstable : ghosts > 0 ? lit : empty))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        blockModels.registerSimpleItemModel(unit, ModelLocationUtils.getModelLocation(unit));
    }

    private static MultiVariant model(BlockModelGenerators blockModels, Block block, String suffix, Material side, Material top, String front) {

        TextureMapping textures = new TextureMapping().put(TextureSlot.SIDE, side).put(TextureSlot.TOP, top)
                .put(TextureSlot.FRONT, new Material(Constants.id(front)));
        return BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(block, suffix, textures, blockModels.modelOutput));
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
