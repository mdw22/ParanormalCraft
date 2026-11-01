package io.github.mdw22.paranormalcraft.item;

import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.TrapDecay;
import io.github.mdw22.paranormalcraft.capture.TrapIntegrity;
import io.github.mdw22.paranormalcraft.capture.TrapSeal;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A Filled Ghost Trap as an item: places the filled trap with its ghost. The tooltip names the ghost, who caught it,
 * where it came from and the trap's integrity, which the item bar shows too (green to red). Carried, it decays like a
 * placed trap ({@code TrapDecay}): silent until 50%, rattling more urgently below, and at 0% the ghost bursts out.
 */
public class FilledGhostTrapItem extends BlockItem {

    public FilledGhostTrapItem(Block block, Properties properties) {

        super(block, properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {

        return stack.has(ModDataComponents.TRAP_SEAL.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {

        return Math.round(13.0F * clientIntegrity(stack) / 100.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {

        return TrapIntegrity.barColour(clientIntegrity(stack));
    }

    private static float clientIntegrity(ItemStack stack) {

        TrapSeal seal = stack.get(ModDataComponents.TRAP_SEAL.get());
        return seal == null ? 100.0F : TrapIntegrity.integrity(seal, TrapIntegrity.clientNow());
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {

        if (!TrapDecay.isFilledTrap(stack)) {
            return;
        }
        TrapSeal seal = TrapDecay.sealOf(stack, level);
        float integrity = TrapIntegrity.integrity(seal, level.getGameTime());
        if (integrity <= 0.0F) {
            CapturedGhost captured = stack.get(ModDataComponents.CAPTURED_GHOST.get());
            int count = stack.getCount();
            stack.shrink(count);
            if (captured != null) {
                TrapDecay.escape(level, captured, owner.position(), "inventory");
            }
            ItemStack empty = new ItemStack(ModItems.GHOST_TRAP.get(), count);
            if (!(owner instanceof Player player) || !player.getInventory().add(empty)) {
                owner.spawnAtLocation(level, empty);
            }
            return;
        }
        TrapIntegrity.Rattle rattle = TrapIntegrity.rattle(integrity, false);
        if (level.getRandom().nextDouble() < rattle.chancePerTick()) {
            TrapDecay.rattleAt(level, owner.position(), rattle, integrity);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {

        CapturedGhost ghost = stack.get(ModDataComponents.CAPTURED_GHOST.get());
        if (ghost == null) {
            builder.accept(Component.translatable("item.paranormalcraft.filled_ghost_trap.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        Identifier typeId = Identifier.tryParse(ghost.entityType());
        Component type = typeId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)
                ? BuiltInRegistries.ENTITY_TYPE.getValue(typeId).getDescription()
                : Component.literal(ghost.entityType());
        builder.accept(Component.translatable("item.paranormalcraft.filled_ghost_trap.ghost", type,
                Component.literal(ghost.name()).withStyle(ChatFormatting.ITALIC)).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("item.paranormalcraft.filled_ghost_trap.captor", ghost.captor()).withStyle(ChatFormatting.GRAY));
        if (stack.has(ModDataComponents.TRAP_SEAL.get())) {
            int percent = TrapIntegrity.percent(clientIntegrity(stack));
            builder.accept(Component.translatable("item.paranormalcraft.filled_ghost_trap.integrity", percent)
                    .withStyle(percent < TrapIntegrity.URGENT_BELOW ? ChatFormatting.RED : percent < TrapIntegrity.CARRIED_RATTLE_BELOW ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        }
        ghost.structure().ifPresent(structure -> builder.accept(Component.translatable("item.paranormalcraft.filled_ghost_trap.from",
                Component.translatable("structure.paranormalcraft." + structure)).withStyle(ChatFormatting.GRAY)));
    }
}
