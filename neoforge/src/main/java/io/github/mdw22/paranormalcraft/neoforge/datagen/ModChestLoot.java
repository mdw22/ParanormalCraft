package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

/**
 * {@code paranormalcraft:chests/abandoned_cabin}: enough for a first set of tools (copper always, leather, amethyst,
 * tinted glass sometimes) plus cabin clutter. One pool per item so each chance is independent.
 */
public class ModChestLoot implements LootTableSubProvider {

    public ModChestLoot(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {

        output.accept(ModStructures.ABANDONED_CABIN_CHEST, LootTable.lootTable()
                .withPool(item(Items.COPPER_INGOT, 3, 7, 1.0F))
                .withPool(item(Items.CANDLE, 1, 3, 1.0F))
                .withPool(item(Items.PAPER, 1, 4, 1.0F))
                .withPool(item(Items.BOOK, 1, 1, 0.5F))
                .withPool(item(Items.LEATHER, 1, 3, 0.6F))
                .withPool(item(Items.AMETHYST_SHARD, 1, 2, 0.3F))
                .withPool(item(Items.TINTED_GLASS, 1, 1, 0.15F))
                .withPool(item(Items.BREAD, 1, 3, 1.0F))
                .withPool(item(Items.STRING, 1, 4, 1.0F))
                .withPool(item(Items.ROTTEN_FLESH, 0, 2, 1.0F)));
    }

    private static LootPool.Builder item(Item item, int min, int max, float chance) {

        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max))));
        if (chance < 1.0F) {
            pool.when(LootItemRandomChanceCondition.randomChance(chance));
        }
        return pool;
    }
}
