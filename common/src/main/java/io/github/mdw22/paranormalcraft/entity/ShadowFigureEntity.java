package io.github.mdw22.paranormalcraft.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationPlacement;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shadow hallucination: a tall, thin silhouette that exists only in one client's level. It is never spawned on
 * the server, can't be picked or hit, casts no shadow, has no hitbox outline and makes no sound when it appears. It
 * vanishes when the watcher looks within 20° of it or after 6 s: snapping away with a quiet sting at {@code full}
 * intensity, fading over 10 ticks at {@code reduced}.
 */
public class ShadowFigureEntity extends Entity implements GeoEntity {

    public static final int LIFETIME_TICKS = 120;
    public static final int FADE_TICKS = 10;
    /** Eye-ish height used for the look check. */
    private static final double FOCUS_HEIGHT = 1.6;

    private static final RawAnimation SWAY = RawAnimation.begin().thenLoop("sway");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private boolean reduced;
    private int fadeTicksLeft = -1;
    private @Nullable Player watcher;

    public ShadowFigureEntity(EntityType<? extends ShadowFigureEntity> type, Level level) {

        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * Client side: who this figure is for, and whether it fades ({@code reduced}) or snaps away ({@code full}).
     */
    public void setup(Player watcher, boolean reduced) {

        this.watcher = watcher;
        this.reduced = reduced;
    }

    public boolean isFading() {

        return this.fadeTicksLeft >= 0;
    }

    /**
     * Opacity multiplier: 1 normally, falling to 0 while fading.
     */
    public float fade(float partialTick) {

        return isFading() ? Math.max(0.0F, (this.fadeTicksLeft - partialTick) / FADE_TICKS) : 1.0F;
    }

    @Override
    public void tick() {

        super.tick();
        if (!this.level().isClientSide() || this.watcher == null) {
            // Never meant to exist on a server.
            this.discard();
            return;
        }
        if (isFading()) {
            if (--this.fadeTicksLeft <= 0) {
                this.discard();
            }
            return;
        }
        if (this.tickCount >= LIFETIME_TICKS || isLookedAt()) {
            vanish();
        }
    }

    private boolean isLookedAt() {

        Vec3 look = this.watcher.getViewVector(1.0F);
        Vec3 toFigure = this.position().add(0, FOCUS_HEIGHT, 0).subtract(this.watcher.getEyePosition());
        return HallucinationPlacement.angleBetween(look.x, look.y, look.z, toFigure.x, toFigure.y, toFigure.z) < HallucinationPlacement.VANISH_VIEW_ANGLE;
    }

    private void vanish() {

        if (this.reduced) {
            this.fadeTicksLeft = FADE_TICKS;
            return;
        }
        this.level().playLocalSound(this.getX(), this.getY() + 1.2, this.getZ(), ModSounds.SHADOW_FIGURE_VANISH.get(), SoundSource.HOSTILE, 0.4F, 1.0F, false);
        this.discard();
    }

    @Override
    public boolean isPickable() {

        return false;
    }

    @Override
    public boolean isInvisible() {

        // Keeps the F3+B hitbox and the shadow away; the renderer draws the figure regardless.
        return true;
    }

    @Override
    public boolean isPushable() {

        return false;
    }

    @Override
    public boolean shouldBeSaved() {

        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<ShadowFigureEntity>("sway", 0, test -> test.setAndContinue(SWAY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {

        return this.animationCache;
    }
}
