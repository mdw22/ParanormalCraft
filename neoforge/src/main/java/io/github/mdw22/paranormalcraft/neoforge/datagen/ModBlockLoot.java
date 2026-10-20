package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.Set;

/**
 * Block drops: an armed Ghost Trap drops itself; a Filled Ghost Trap drops the filled item with its ghost
 * ({@code paranormalcraft:captured_ghost} copied from the block entity).
 */
public class ModBlockLoot extends BlockLootSubProvider {

    public ModBlockLoot(HolderLookup.Provider registries) {

        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {

        dropSelf(ModBlocks.GHOST_TRAP.get());
        dropSelf(ModBlocks.CONTAINMENT_UNIT.get());
        dropSelf(ModBlocks.WARD_LANTERN.get());
        add(ModBlocks.FILLED_GHOST_TRAP.get(), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModItems.FILLED_GHOST_TRAP.get())
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(ModDataComponents.CAPTURED_GHOST.get())))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {

        return List.of(ModBlocks.GHOST_TRAP.get(), ModBlocks.FILLED_GHOST_TRAP.get(), ModBlocks.CONTAINMENT_UNIT.get(), ModBlocks.WARD_LANTERN.get());
    }
}
