package io.github.mdw22.paranormalcraft.item;

import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.capture.WandHeat;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Hold use (like drawing a bow) to fire a beam that latches onto a visible ghost and drains it; see
 * {@link TetherManager}. Heat builds while firing and shows on the item bar (durability shows when it's cold); at 8 s it
 * overheats for 4 s. Durability is seconds of beam time; repair with copper ingots.
 */
public class TetherWandItem extends Item {

    private static final int USE_DURATION = 72_000;
    private static final int COOL_COLOUR = 0xFFC040;
    private static final int HOT_COLOUR = 0xFF3010;

    public TetherWandItem(Item.Properties properties) {

        super(properties);
    }

    public static WandHeat heat(ItemStack stack) {

        return stack.getOrDefault(ModDataComponents.WAND_HEAT.get(), WandHeat.COLD);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        if (!heat(stack).canFire()) {
            if (!level.isClientSide()) {
                level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.TETHER_OVERHEAT.get(), SoundSource.PLAYERS, 0.4F, 1.4F);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {

        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {

        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {

        if (entity instanceof ServerPlayer player) {
            TetherManager.fire(player, stack);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {

        if (entity instanceof ServerPlayer player) {
            TetherManager.release(player);
        }
        return true;
    }

    /**
     * Cools by 2 a tick whenever it isn't firing.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {

        WandHeat heat = heat(stack);
        if (heat.heat() == 0 && heat.lockout() == 0) {
            return;
        }
        if (owner instanceof Player player && TetherManager.isFiring(player) && TetherManager.heldWand(player) == stack) {
            return;
        }
        stack.set(ModDataComponents.WAND_HEAT.get(), heat.cool());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {

        return heat(stack).heat() > 0 || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {

        WandHeat heat = heat(stack);
        return heat.heat() > 0 ? Math.round(13.0F * heat.fraction()) : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {

        WandHeat heat = heat(stack);
        if (heat.heat() == 0) {
            return super.getBarColor(stack);
        }
        return heat.overheated() ? HOT_COLOUR : ARGB.srgbLerp(heat.fraction(), COOL_COLOUR, HOT_COLOUR) & 0xFFFFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {

        WandHeat heat = heat(stack);
        int secondsLeft = stack.getMaxDamage() - stack.getDamageValue();
        builder.accept(Component.translatable("item.paranormalcraft.tether_wand.beam_time", secondsLeft).withStyle(ChatFormatting.GRAY));
        if (heat.overheated()) {
            builder.accept(Component.translatable("item.paranormalcraft.tether_wand.overheated").withStyle(ChatFormatting.RED));
        } else if (heat.heat() > 0) {
            builder.accept(Component.translatable("item.paranormalcraft.tether_wand.heat", Mth.floor(heat.fraction() * 100.0F)).withStyle(ChatFormatting.GOLD));
        }
    }
}
