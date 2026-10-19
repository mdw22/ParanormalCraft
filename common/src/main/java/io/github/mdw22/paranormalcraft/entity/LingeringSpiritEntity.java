package io.github.mdw22.paranormalcraft.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostCuriousGoal;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostReturnHomeGoal;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostTetheredGoal;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostShyGoal;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostWanderGoal;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The first ghost: a shy sheet-ghost that wanders near where it was placed and shows itself now and then. It never
 * attacks.
 */
public class LingeringSpiritEntity extends GhostEntity implements GeoEntity {

    /** Render-state flag the renderer fills so the manifest controller can react. */
    public static final DataTicket<Boolean> MANIFESTING = DataTicket.create("paranormalcraft_manifesting", Boolean.class);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation MANIFEST = RawAnimation.begin().thenPlayAndHold("manifest");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public LingeringSpiritEntity(EntityType<? extends LingeringSpiritEntity> type, Level level) {

        super(type, level);
    }

    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(0, new GhostTetheredGoal(this, 1.2));
        this.goalSelector.addGoal(0, new GhostReturnHomeGoal(this, 1.2));
        this.goalSelector.addGoal(1, new GhostShyGoal(this, 1.4));
        this.goalSelector.addGoal(2, new GhostCuriousGoal(this, 1.0, 200));
        this.goalSelector.addGoal(3, new GhostWanderGoal(this, 0.6, 60));
    }

    @Override
    protected SoundEvent getWhisperSound() {

        return ModSounds.LINGERING_SPIRIT_WHISPER.get();
    }

    @Override
    protected SoundEvent getDisperseSound() {

        return ModSounds.LINGERING_SPIRIT_DISPERSE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {

        return ModSounds.LINGERING_SPIRIT_HURT.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<LingeringSpiritEntity>("idle", 5, test -> test.setAndContinue(IDLE)));
        controllers.add(new AnimationController<LingeringSpiritEntity>("manifest", 0, test -> {
            if (Boolean.TRUE.equals(test.getData(MANIFESTING))) {
                return test.setAndContinue(MANIFEST);
            }
            test.controller().reset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return this.animationCache;
    }
}
