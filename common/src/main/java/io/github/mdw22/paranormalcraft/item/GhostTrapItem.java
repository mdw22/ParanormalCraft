package io.github.mdw22.paranormalcraft.item;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.entity.ThrownGhostTrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Ghost Trap. Right-click throws it in a short arc and it arms where it lands; sneak + right-click on a block places
 * it on that face, armed.
 */
public class GhostTrapItem extends BlockItem {

    private static final float THROW_SPEED = Balance.Trap.THROW_SPEED;

    public GhostTrapItem(Block block, Properties properties) {

        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        if (context.getPlayer() != null && !context.isSecondaryUseActive()) {
            return use(context.getLevel(), context.getPlayer(), context.getHand());
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.6F);
        if (level instanceof ServerLevel serverLevel) {
            ThrownGhostTrap trap = new ThrownGhostTrap(serverLevel, player, stack.copyWithCount(1));
            trap.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_SPEED, 1.0F);
            serverLevel.addFreshEntity(trap);
        }
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
