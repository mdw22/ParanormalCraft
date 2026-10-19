package io.github.mdw22.paranormalcraft.item;

import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * A Filled Ghost Trap as an item: places the filled trap with its ghost. The tooltip names the ghost, who caught it
 * and where it came from.
 */
public class FilledGhostTrapItem extends BlockItem {

    public FilledGhostTrapItem(Block block, Properties properties) {

        super(block, properties);
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
        ghost.structure().ifPresent(structure -> builder.accept(Component.translatable("item.paranormalcraft.filled_ghost_trap.from",
                Component.translatable("structure.paranormalcraft." + structure)).withStyle(ChatFormatting.GRAY)));
    }
}
