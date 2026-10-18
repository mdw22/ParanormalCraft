package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GameTests for items: which books the goggles take in an anvil, and the cabin chest's loot ranges.
 */
public final class ItemGameTests {

    private static final int LOOT_ROLLS = 200;
    /**
     * What the EMF Reader and Spirit Thermometer need together: 7 + 1 copper; 1 + 1 redstone, plus 1 for the EMF
     * Reader's redstone torch. One chest must always cover both.
     */
    private static final int COPPER_FOR_BOTH_TOOLS = 8;
    private static final int REDSTONE_FOR_BOTH_TOOLS = 3;

    /** Item -> {min, max, always}: the cabin chest's ranges (always = 1 means it must be in every roll). */
    private static final Map<Item, int[]> CABIN_LOOT = new LinkedHashMap<>();

    static {
        CABIN_LOOT.put(Items.COPPER_INGOT, new int[]{8, 10, 1});
        CABIN_LOOT.put(Items.REDSTONE, new int[]{3, 4, 1});
        CABIN_LOOT.put(Items.CANDLE, new int[]{1, 3, 1});
        CABIN_LOOT.put(Items.PAPER, new int[]{1, 4, 1});
        CABIN_LOOT.put(Items.BREAD, new int[]{1, 3, 1});
        CABIN_LOOT.put(Items.STRING, new int[]{1, 4, 1});
        CABIN_LOOT.put(Items.BOOK, new int[]{1, 1, 0});
        CABIN_LOOT.put(Items.LEATHER, new int[]{1, 3, 0});
        CABIN_LOOT.put(Items.AMETHYST_SHARD, new int[]{1, 2, 0});
        CABIN_LOOT.put(Items.TINTED_GLASS, new int[]{1, 1, 0});
        CABIN_LOOT.put(Items.ROTTEN_FLESH, new int[]{1, 2, 0});
    }

    private ItemGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("goggles_take_only_curses_in_anvil", ItemGameTests::gogglesTakeOnlyCurses);
        RegistrationHelper.testFunction("cabin_loot_ranges", ItemGameTests::cabinLootRanges);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    /**
     * A survival player's anvil: Binding and Vanishing books go on, Protection, Unbreaking and Mending don't.
     */
    private static void gogglesTakeOnlyCurses(GameTestHelper helper) {

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        Map<ResourceKey<Enchantment>, Boolean> expected = new LinkedHashMap<>();
        expected.put(Enchantments.BINDING_CURSE, true);
        expected.put(Enchantments.VANISHING_CURSE, true);
        expected.put(Enchantments.PROTECTION, false);
        expected.put(Enchantments.UNBREAKING, false);
        expected.put(Enchantments.MENDING, false);
        for (Map.Entry<ResourceKey<Enchantment>, Boolean> entry : expected.entrySet()) {
            Holder<Enchantment> enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(entry.getKey());
            AnvilMenu anvil = new AnvilMenu(0, player.getInventory());
            anvil.getSlot(0).set(new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
            anvil.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1)));
            ItemStack result = anvil.getSlot(2).getItem();
            boolean applied = !result.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(enchantment, result) == 1;
            String name = entry.getKey().identifier().getPath();
            check(helper, applied == entry.getValue(), "%s book %s", name, applied ? "was accepted" : "was refused");
            check(helper, !applied || anvil.getCost() > 0, "%s: no level cost", name);
        }
        helper.succeed();
    }

    /**
     * Rolls {@code paranormalcraft:chests/abandoned_cabin} 200 times: every item's count stays in its range, the
     * "always" items are in every roll, every roll covers both the EMF Reader and the thermometer recipes, and the
     * copper and redstone ranges are hit at both ends.
     */
    private static void cabinLootRanges(GameTestHelper helper) {

        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ModStructures.ABANDONED_CABIN_CHEST);
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        Map<Item, int[]> seen = new HashMap<>();
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            Map<Item, Integer> counts = new HashMap<>();
            List<ItemStack> items = table.getRandomItems(params);
            for (ItemStack stack : items) {
                if (stack.isEmpty()) {
                    continue; // a count roll of 0, e.g. rotten flesh
                }
                check(helper, CABIN_LOOT.containsKey(stack.getItem()), "unexpected item %s", stack);
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
            int copper = counts.getOrDefault(Items.COPPER_INGOT, 0);
            int redstone = counts.getOrDefault(Items.REDSTONE, 0);
            check(helper, copper >= COPPER_FOR_BOTH_TOOLS && redstone >= REDSTONE_FOR_BOTH_TOOLS,
                    "roll %d: %d copper and %d redstone don't cover the EMF Reader and thermometer", roll, copper, redstone);
            for (Map.Entry<Item, int[]> entry : CABIN_LOOT.entrySet()) {
                int count = counts.getOrDefault(entry.getKey(), 0);
                int[] range = entry.getValue();
                String name = entry.getKey().toString();
                if (count == 0) {
                    check(helper, range[2] == 0, "roll %d: no %s", roll, name);
                    continue;
                }
                check(helper, count >= range[0] && count <= range[1], "roll %d: %d %s, expected %d-%d", roll, count, name, range[0], range[1]);
                int[] minMax = seen.computeIfAbsent(entry.getKey(), item -> new int[]{Integer.MAX_VALUE, 0});
                minMax[0] = Math.min(minMax[0], count);
                minMax[1] = Math.max(minMax[1], count);
            }
        }
        for (Item item : List.of(Items.COPPER_INGOT, Items.REDSTONE)) {
            int[] range = CABIN_LOOT.get(item);
            int[] minMax = seen.get(item);
            check(helper, minMax[0] == range[0] && minMax[1] == range[1], "%s rolled %d-%d over %d rolls, expected %d-%d",
                    item, minMax[0], minMax[1], LOOT_ROLLS, range[0], range[1]);
        }
        Constants.LOG.info("Cabin loot over {} rolls: copper {}-{}, redstone {}-{}", LOOT_ROLLS,
                seen.get(Items.COPPER_INGOT)[0], seen.get(Items.COPPER_INGOT)[1], seen.get(Items.REDSTONE)[0], seen.get(Items.REDSTONE)[1]);
        helper.succeed();
    }
}
