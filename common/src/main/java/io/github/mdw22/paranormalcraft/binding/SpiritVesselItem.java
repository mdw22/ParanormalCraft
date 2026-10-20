package io.github.mdw22.paranormalcraft.binding;

import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.familiar.Familiars;
import io.github.mdw22.paranormalcraft.familiar.WispFamiliarEntity;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The Spirit Vessel. Empty, it goes into a Containment Unit's Vessel slot to bind a stored ghost; bound, it carries
 * the ghost's full data ({@code paranormalcraft:captured_ghost}, as a Filled Ghost Trap does) and does that ghost's
 * job ({@link BoundJobs}). For the Wisp Familiar: use to summon or recall, sneak + use while it is out to guide.
 */
public class SpiritVesselItem extends Item {

    public SpiritVesselItem(Item.Properties properties) {

        super(properties);
    }

    public static boolean isBound(ItemStack stack) {

        return stack.getItem() instanceof SpiritVesselItem && stack.has(ModDataComponents.CAPTURED_GHOST.get());
    }

    public static boolean isEmptyVessel(ItemStack stack) {

        return stack.getItem() instanceof SpiritVesselItem && !stack.has(ModDataComponents.CAPTURED_GHOST.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        CapturedGhost ghost = stack.get(ModDataComponents.CAPTURED_GHOST.get());
        BoundJobs.Job job = ghost == null ? null : BoundJobs.jobFor(ghost.entityType());
        if (job == null || job.use() != BoundJobs.Use.CARRIED) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && job.id().equals(BoundJobs.WISP_FAMILIAR)) {
            WispFamiliarEntity out = Familiars.of(serverPlayer);
            if (player.isShiftKeyDown() && out != null && ghost.id().equals(out.ghost())) {
                Familiars.guide(serverPlayer);
            } else {
                Familiars.toggle(serverPlayer, stack);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {

        CapturedGhost ghost = stack.get(ModDataComponents.CAPTURED_GHOST.get());
        if (ghost == null) {
            builder.accept(Component.translatable("item.paranormalcraft.spirit_vessel.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        Identifier typeId = Identifier.tryParse(ghost.entityType());
        Component type = typeId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)
                ? BuiltInRegistries.ENTITY_TYPE.getValue(typeId).getDescription()
                : Component.literal(ghost.entityType());
        builder.accept(Component.translatable("item.paranormalcraft.spirit_vessel.bound",
                Component.literal(ghost.name()).withStyle(ChatFormatting.ITALIC), type).withStyle(ChatFormatting.AQUA));
        BoundJobs.Job job = BoundJobs.jobFor(ghost.entityType());
        if (job != null) {
            builder.accept(Component.translatable(job.langKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
            builder.accept(Component.translatable(job.langKey() + ".usage").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {

        return stack.has(ModDataComponents.CAPTURED_GHOST.get())
                ? Component.translatable("item.paranormalcraft.bound_spirit_vessel")
                : super.getName(stack);
    }
}
