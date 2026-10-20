package io.github.mdw22.paranormalcraft.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Restful, the Lingering Spirit's blessing for releasing it: while it lasts, phantoms don't come for the player. It
 * keeps resetting the player's time-since-rest statistic, which is what vanilla's phantom spawner reads, so no spawn
 * hook is needed.
 */
public class RestfulEffect extends MobEffect {

    public RestfulEffect() {

        super(MobEffectCategory.BENEFICIAL, 0x9CF2F2);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {

        return tickCount % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {

        if (mob instanceof ServerPlayer player) {
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        }
        return true;
    }
}
