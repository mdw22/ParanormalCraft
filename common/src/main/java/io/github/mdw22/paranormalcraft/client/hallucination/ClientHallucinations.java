package io.github.mdw22.paranormalcraft.client.hallucination;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ClientConfig;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.entity.ShadowFigureEntity;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationKind;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationPlacement;
import io.github.mdw22.paranormalcraft.network.HallucinationPayload;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Client side of hallucinations: places and spawns a client-only {@link ShadowFigureEntity}. At most one at a time;
 * skipped silently when hallucinations are off, the FOV is too narrow, or no valid spot is found in {@link #ATTEMPTS} tries.
 */
public final class ClientHallucinations {

    public static final int ATTEMPTS = 5;
    /** Spots this dark or darker are preferred. */
    public static final int MAX_BLOCK_LIGHT = 7;
    /** Client-only entity ids count down from here so they never clash with server ids. */
    private static int nextClientId = -1_000_000;
    private static @Nullable ShadowFigureEntity active;

    private ClientHallucinations() {
    }

    /**
     * The figure currently shown, if any (a test hook).
     */
    public static @Nullable ShadowFigureEntity active() {

        return active != null && !active.isRemoved() ? active : null;
    }

    public static void handle(HallucinationPayload payload) {

        ClientConfig.HallucinationIntensity intensity = ModConfigs.client().hallucinationIntensity();
        Minecraft minecraft = Minecraft.getInstance();
        if (intensity == ClientConfig.HallucinationIntensity.OFF || active() != null || minecraft.level == null || minecraft.player == null
                || payload.kind() != HallucinationKind.SHADOW_FIGURE) {
            return;
        }
        boolean reduced = intensity == ClientConfig.HallucinationIntensity.REDUCED;
        HallucinationPlacement.Band band = HallucinationPlacement.band(halfHorizontalFov(minecraft));
        if (band == null) {
            Constants.LOG.debug("FOV too narrow for a shadow figure; skipped");
            return;
        }
        Vec3 spot = findSpot(minecraft.level, minecraft.player, band, reduced, RandomSource.create(payload.seed()));
        if (spot == null) {
            Constants.LOG.debug("No room for a shadow figure; skipped");
            return;
        }
        ShadowFigureEntity figure = new ShadowFigureEntity(ModEntities.SHADOW_FIGURE.get(), minecraft.level);
        figure.setId(nextClientId--);
        figure.setup(minecraft.player, reduced);
        Vec3 toPlayer = minecraft.player.position().subtract(spot);
        float yaw = (float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
        figure.snapTo(spot.x, spot.y, spot.z, yaw, 0.0F);
        figure.setYHeadRot(yaw);
        minecraft.level.addEntity(figure);
        active = figure;
        Constants.LOG.info("Shadow figure at {} ({} degrees off the view, {} blocks away)", BlockPos.containing(spot).toShortString(),
                Math.round(HallucinationPlacement.relativeAngle(minecraft.player.getYRot(), spot.x - minecraft.player.getX(), spot.z - minecraft.player.getZ())),
                Math.round(Math.hypot(spot.x - minecraft.player.getX(), spot.z - minecraft.player.getZ())));
    }

    /**
     * The horizontal half-FOV of the current window, from the FOV setting and the window's aspect ratio.
     */
    public static double halfHorizontalFov(Minecraft minecraft) {

        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        return HallucinationPlacement.halfHorizontalFov(minecraft.options.fov().get(), aspect);
    }

    /**
     * Up to {@link #ATTEMPTS} tries; the first dark spot (feet at block light ≤ {@link #MAX_BLOCK_LIGHT}) wins,
     * otherwise the first valid one.
     */
    private static @Nullable Vec3 findSpot(ClientLevel level, LocalPlayer player, HallucinationPlacement.Band band, boolean reduced,
                                           RandomSource random) {

        Vec3 eye = player.getEyePosition();
        Vec3 fallback = null;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double angle = HallucinationPlacement.sampleAngle(band, random.nextBoolean(), random.nextDouble());
            double distance = HallucinationPlacement.sampleDistance(reduced, random.nextDouble());
            HallucinationPlacement.Offset offset = HallucinationPlacement.offset(player.getYRot(), angle, distance);
            BlockPos column = BlockPos.containing(player.getX() + offset.dx(), player.getY(), player.getZ() + offset.dz());
            BlockPos feet = standableNear(level, column);
            if (feet == null) {
                continue;
            }
            Vec3 spot = Vec3.atBottomCenterOf(feet);
            BlockHitResult sight = level.clip(new ClipContext(eye, spot.add(0, 1.6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (sight.getType() != HitResult.Type.MISS) {
                continue;
            }
            if (level.getBrightness(LightLayer.BLOCK, feet) <= MAX_BLOCK_LIGHT) {
                return spot;
            }
            if (fallback == null) {
                fallback = spot;
            }
        }
        return fallback;
    }

    /**
     * A spot near the player's height with solid ground and air for the figure's full height (3 blocks).
     */
    private static @Nullable BlockPos standableNear(ClientLevel level, BlockPos column) {

        for (int dy = 3; dy >= -4; dy--) {
            BlockPos feet = column.above(dy);
            BlockPos below = feet.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && level.getBlockState(feet.above(2)).getCollisionShape(level, feet.above(2)).isEmpty()) {
                return feet;
            }
        }
        return null;
    }

    public static void reset() {

        active = null;
    }
}
