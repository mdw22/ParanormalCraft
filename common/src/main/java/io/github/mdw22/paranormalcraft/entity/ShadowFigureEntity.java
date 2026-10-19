package io.github.mdw22.paranormalcraft.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.mdw22.paranormalcraft.debug.trace.ClientTraceHook;
import io.github.mdw22.paranormalcraft.hallucination.FigureSounds;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationPlacement;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shadow hallucination: a tall, thin silhouette that exists only in one client's level. It is never spawned on
 * the server, can't be picked or hit, casts no shadow and has no hitbox outline. While it stands there it makes a
 * quiet presence sound at its own position, for its watcher only (see {@link FigureSounds}), so it can be heard and
 * located. It vanishes when the watcher looks within 20° of it with it in sight, or 10 s after it was placed: snapping away with a quiet sting at
 * {@code full} intensity, fading over 10 ticks with no sting at {@code reduced}.
 */
public class ShadowFigureEntity extends Entity implements GeoEntity {

    public static final int LIFETIME_TICKS = 200;
    public static final int FADE_TICKS = 10;
    /** Eye-ish height used for the look check. */
    private static final double FOCUS_HEIGHT = 1.6;
    /** The presence sound plays from about head height. */
    private static final double SOUND_HEIGHT = 1.6;

    private static final RawAnimation SWAY = RawAnimation.begin().thenLoop("sway");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private boolean reduced;
    private int fadeTicksLeft = -1;
    private @Nullable Player watcher;
    private int nextSoundTick;
    /** Whether it has been on screen and in sight yet (a sound-first figure starts unseen). */
    private boolean seen;
    /** Why it vanished, for the client trace: {@code looked_at} or {@code timeout}. */
    private @Nullable String vanishReason;

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

    public boolean wasSeen() {

        return this.seen;
    }

    public void markSeen() {

        this.seen = true;
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
        boolean lookedAt = isLookedAt();
        if (lookedAt && !this.seen) {
            // Turning straight to it shows it and makes it vanish in the same tick: it was seen.
            this.seen = true;
            ClientTraceHook.event("figure_seen", line -> {
                ClientTraceHook.addPos(line, this.getX(), this.getY(), this.getZ());
                line.addProperty("age", this.tickCount);
            });
        }
        if (this.tickCount >= LIFETIME_TICKS || lookedAt) {
            this.vanishReason = lookedAt ? "looked_at" : "timeout";
            vanish();
            return;
        }
        int age = this.tickCount - 1;
        if (FigureSounds.due(age, this.nextSoundTick, false)) {
            ClientTraceHook.markNextSoundLocal();
            float volume = FigureSounds.volume(this.reduced);
            this.level().playLocalSound(this.getX(), this.getY() + SOUND_HEIGHT, this.getZ(), ModSounds.SHADOW_FIGURE_PRESENCE.get(), SoundSource.HOSTILE,
                    volume, 1.0F, false);
            // Also traced directly, so it shows up even where there's no audio device (CI) to report played sounds.
            ClientTraceHook.event("figure_sound", line -> {
                ClientTraceHook.addPos(line, this.getX(), this.getY() + SOUND_HEIGHT, this.getZ());
                line.addProperty("volume", volume);
                line.addProperty("age", age);
            });
            this.nextSoundTick = age + FigureSounds.nextInterval(this.reduced, this.random.nextDouble());
        }
    }

    /**
     * Within 20° of the view and actually in sight: a sound-first figure behind a wall doesn't vanish when the player
     * turns toward the wall.
     */
    private boolean isLookedAt() {

        Vec3 eye = this.watcher.getEyePosition();
        Vec3 focus = this.position().add(0, FOCUS_HEIGHT, 0);
        Vec3 look = this.watcher.getViewVector(1.0F);
        Vec3 toFigure = focus.subtract(eye);
        if (HallucinationPlacement.angleBetween(look.x, look.y, look.z, toFigure.x, toFigure.y, toFigure.z) >= HallucinationPlacement.VANISH_VIEW_ANGLE) {
            return false;
        }
        return this.level().clip(new ClipContext(eye, focus, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.watcher)).getType() == HitResult.Type.MISS;
    }

    private void vanish() {

        ClientTraceHook.event("figure_vanish", line -> {
            ClientTraceHook.addPos(line, this.getX(), this.getY(), this.getZ());
            line.addProperty("reason", String.valueOf(this.vanishReason));
            line.addProperty("age", this.tickCount);
            line.addProperty("reduced", this.reduced);
        });
        if (this.reduced) {
            this.fadeTicksLeft = FADE_TICKS;
            return;
        }
        ClientTraceHook.markNextSoundLocal();
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
