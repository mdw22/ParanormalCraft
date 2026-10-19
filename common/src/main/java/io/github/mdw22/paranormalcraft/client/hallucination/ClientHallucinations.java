package io.github.mdw22.paranormalcraft.client.hallucination;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.config.ClientConfig;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.entity.ShadowFigureEntity;
import io.github.mdw22.paranormalcraft.hallucination.FigureRetry;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationKind;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationPlacement;
import io.github.mdw22.paranormalcraft.network.FigureResultPayload;
import io.github.mdw22.paranormalcraft.network.HallucinationPayload;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Client side of hallucinations: places and spawns a client-only {@link ShadowFigureEntity}, one at a time.
 * <p>
 * A request is tried at once and, if no spot qualifies, again every 10 ticks as the player turns, for up to 10 s
 * ({@link FigureRetry}). A visible spot is one on screen (inside the FOV band) that the player can see. Under a roof
 * every angle and distance in the band is scanned; failing that, a closer spot in the next room counts if it is seen
 * through an open doorway. Indoors, after 2 s of failed tries, the figure is placed "sound first": standable but not on screen,
 * preferably outside where a window or doorway will show it when the player turns, otherwise behind a wall; its
 * presence sound tells the player something is out there. The result goes to the trace ({@code figure_result}) and,
 * for {@code debug hallucinate}, back to the server and into chat.
 */
public final class ClientHallucinations {

    public static final int ATTEMPTS = 5;
    /** Spots this dark or darker are preferred. */
    public static final int MAX_BLOCK_LIGHT = 7;
    /** Indoor scan: angle step through the band, in degrees. */
    private static final double SCAN_ANGLE_STEP = 2.0;
    /** Sound-first scan: angle step all the way round, in degrees. */
    private static final double ROUND_ANGLE_STEP = 10.0;
    /** Client-only entity ids count down from here so they never clash with server ids. */
    private static int nextClientId = -1_000_000;
    private static @Nullable ShadowFigureEntity active;
    private static @Nullable Request pending;

    /** A figure waiting for a spot. */
    private static final class Request {

        private final RandomSource random;
        private final boolean reduced;
        private final boolean report;
        private int age;

        private Request(long seed, boolean reduced, boolean report) {

            this.random = RandomSource.create(seed);
            this.reduced = reduced;
            this.report = report;
        }
    }

    /** Where a figure goes and whether the player can see it there. */
    private record Spot(Vec3 position, boolean visible) {
    }

    private ClientHallucinations() {
    }

    /**
     * The figure currently shown, if any (a test hook).
     */
    public static @Nullable ShadowFigureEntity active() {

        return active != null && !active.isRemoved() ? active : null;
    }

    public static void handle(HallucinationPayload payload) {

        Minecraft minecraft = Minecraft.getInstance();
        if (payload.kind() != HallucinationKind.SHADOW_FIGURE || minecraft.level == null || minecraft.player == null) {
            return;
        }
        ClientConfig.HallucinationIntensity intensity = ModConfigs.client().hallucinationIntensity();
        if (intensity == ClientConfig.HallucinationIntensity.OFF) {
            result(payload.report(), "skipped: hallucinations are off");
            return;
        }
        if (active() != null || pending != null) {
            result(payload.report(), "skipped: a figure is already showing");
            return;
        }
        pending = new Request(payload.seed(), intensity == ClientConfig.HallucinationIntensity.REDUCED, payload.report());
        tick(minecraft);
    }

    /**
     * Called every client tick: retries a waiting request, and notes when a placed figure is first seen.
     */
    public static void tick(Minecraft minecraft) {

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ShadowFigureEntity figure = active();
        if (figure != null && !figure.wasSeen() && isOnScreen(minecraft, figure.position())) {
            figure.markSeen();
            ClientTrace.event("figure_seen", line -> {
                ClientTrace.addPos(line, figure);
                line.addProperty("age", figure.tickCount);
            });
        }
        Request request = pending;
        if (request == null) {
            return;
        }
        int age = request.age++;
        FigureRetry.Step step = FigureRetry.step(age, isIndoors(minecraft.level, minecraft.player));
        if (step == FigureRetry.Step.WAIT) {
            return;
        }
        if (step == FigureRetry.Step.GIVE_UP) {
            pending = null;
            result(request.report, "skipped: no spot in " + FigureRetry.GIVE_UP_TICKS / 20 + " s");
            return;
        }
        HallucinationPlacement.Band band = HallucinationPlacement.band(halfHorizontalFov(minecraft));
        if (band == null) {
            pending = null;
            result(request.report, "skipped: field of view too narrow");
            return;
        }
        Spot spot = findVisibleSpot(minecraft.level, minecraft.player, band, request.reduced, request.random);
        if (spot == null && step == FigureRetry.Step.TRY_VISIBLE_THEN_SOUND_FIRST) {
            spot = findSoundFirstSpot(minecraft.level, minecraft.player, band, request.random);
        }
        if (spot == null) {
            return;
        }
        pending = null;
        place(minecraft, spot, request, age);
    }

    private static void place(Minecraft minecraft, Spot spot, Request request, int age) {

        LocalPlayer player = minecraft.player;
        Vec3 at = spot.position();
        ShadowFigureEntity figure = new ShadowFigureEntity(ModEntities.SHADOW_FIGURE.get(), minecraft.level);
        figure.setId(nextClientId--);
        figure.setup(player, request.reduced);
        Vec3 toPlayer = player.position().subtract(at);
        float yaw = (float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
        figure.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        figure.setYHeadRot(yaw);
        minecraft.level.addEntity(figure);
        active = figure;
        if (spot.visible()) {
            figure.markSeen();
        }
        boolean indoors = isIndoors(minecraft.level, player);
        double angle = HallucinationPlacement.relativeAngle(player.getYRot(), at.x - player.getX(), at.z - player.getZ());
        double distance = Math.hypot(at.x - player.getX(), at.z - player.getZ());
        ClientTrace.event("figure_spawn", line -> {
            ClientTrace.addPos(line, figure);
            line.addProperty("angle", Math.round(angle * 10.0) / 10.0);
            line.addProperty("distance", Math.round(distance * 100.0) / 100.0);
            line.addProperty("reduced", request.reduced);
            line.addProperty("indoors", indoors);
            line.addProperty("visible", spot.visible());
            line.addProperty("triesTicks", age);
            line.addProperty("blockLight", minecraft.level.getBrightness(LightLayer.BLOCK, BlockPos.containing(at)));
        });
        Constants.LOG.info("Shadow figure at {} ({} degrees off the view, {} blocks away, {}, after {} ticks)", BlockPos.containing(at).toShortString(),
                Math.round(angle), Math.round(distance), spot.visible() ? "visible" : "sound first", age);
        result(request.report, spot.visible() ? "placed (visible)" : "placed (sound-first)");
    }

    /**
     * Traces the outcome and, if asked, sends it to the server and shows it in chat.
     */
    private static void result(boolean report, String result) {

        ClientTrace.event("figure_result", line -> line.addProperty("result", result));
        if (!report) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("Shadow figure: " + result));
        }
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new FigureResultPayload(result)));
        }
    }

    /**
     * The horizontal half-FOV of the current window, from the FOV setting and the window's aspect ratio.
     */
    public static double halfHorizontalFov(Minecraft minecraft) {

        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        return HallucinationPlacement.halfHorizontalFov(minecraft.options.fov().get(), aspect);
    }

    /**
     * Whether something solid is above the player's head (a roof).
     */
    public static boolean isIndoors(ClientLevel level, LocalPlayer player) {

        BlockPos eye = BlockPos.containing(player.getEyePosition());
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, eye.getX(), eye.getZ()) > eye.getY() + 1;
    }

    /**
     * Whether a figure standing at {@code feet} is on screen (inside the horizontal FOV) and in the player's sight.
     */
    private static boolean isOnScreen(Minecraft minecraft, Vec3 feet) {

        LocalPlayer player = minecraft.player;
        double angle = Math.abs(HallucinationPlacement.relativeAngle(player.getYRot(), feet.x - player.getX(), feet.z - player.getZ()));
        return angle < halfHorizontalFov(minecraft) && canSee(minecraft.level, player, feet);
    }

    private static boolean canSee(ClientLevel level, LocalPlayer player, Vec3 feet) {

        BlockHitResult sight = level.clip(new ClipContext(player.getEyePosition(), feet.add(0, 1.6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return sight.getType() == HitResult.Type.MISS;
    }

    // ---- visible spots ----

    /**
     * A spot on screen (in the band) that the player can see: under a roof, a scan of the whole band at the usual
     * distances, then as close as {@link FigureRetry#INDOOR_MIN_DISTANCE} through an open doorway (the next room);
     * outside, {@link #ATTEMPTS} random tries. Dark spots first.
     */
    private static @Nullable Spot findVisibleSpot(ClientLevel level, LocalPlayer player, HallucinationPlacement.Band band, boolean reduced,
                                                  RandomSource random) {

        double min = reduced ? HallucinationPlacement.REDUCED_MIN_DISTANCE : HallucinationPlacement.NEAR_MIN_DISTANCE;
        double max = reduced ? HallucinationPlacement.REDUCED_MAX_DISTANCE : HallucinationPlacement.NEAR_MAX_DISTANCE;
        Vec3 found;
        if (isIndoors(level, player)) {
            found = scan(level, player, bandAngles(band), min, max, random, spot -> inBandAfterSnapping(player, band, spot) && canSee(level, player, spot));
            if (found == null) {
                // The next room, seen through an open doorway: closer, but only through a door.
                found = scan(level, player, bandAngles(band), FigureRetry.INDOOR_MIN_DISTANCE, max, random,
                        spot -> inBandAfterSnapping(player, band, spot) && canSee(level, player, spot) && throughDoorway(level, player, spot));
            }
        } else {
            found = randomTries(level, player, band, reduced, random);
        }
        return found == null ? null : new Spot(found, true);
    }

    /**
     * Whether the sight line from the player's eyes to a figure standing at {@code feet} passes through a door block
     * (an open door, since the line is clear): the figure stands in another room, seen through the doorway.
     */
    private static boolean throughDoorway(ClientLevel level, LocalPlayer player, Vec3 feet) {

        Vec3 eye = player.getEyePosition();
        Vec3 focus = feet.add(0, 1.6, 0);
        BlockPos eyeBlock = BlockPos.containing(eye);
        int steps = (int) Math.ceil(eye.distanceTo(focus) * 4.0);
        for (int i = 1; i < steps; i++) {
            BlockPos pos = BlockPos.containing(eye.lerp(focus, (double) i / steps));
            if (!pos.equals(eyeBlock) && level.getBlockState(pos).getBlock() instanceof DoorBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * A spot out of sight for now, 6 to 13 blocks out. In order of preference:
     * <ol>
     *     <li>outdoors and visible through an opening, but off screen (turning shows it);</li>
     *     <li>outdoors behind a wall or window inside the band;</li>
     *     <li>outdoors on any side;</li>
     *     <li>anywhere standable.</li>
     * </ol>
     */
    private static @Nullable Spot findSoundFirstSpot(ClientLevel level, LocalPlayer player, HallucinationPlacement.Band band, RandomSource random) {

        double min = FigureRetry.SOUND_FIRST_MIN_DISTANCE;
        double max = FigureRetry.SOUND_FIRST_MAX_DISTANCE;
        List<Double> round = new ArrayList<>();
        for (double angle = -180.0 + ROUND_ANGLE_STEP; angle <= 180.0; angle += ROUND_ANGLE_STEP) {
            round.add(angle);
        }
        Predicate<Vec3> outdoors = spot -> isOpenSky(level, spot);
        Vec3 found = scan(level, player, round, min, max, random, spot -> outdoors.test(spot) && canSee(level, player, spot) && !inBandAfterSnapping(player, band, spot)
                && Math.abs(HallucinationPlacement.relativeAngle(player.getYRot(), spot.x - player.getX(), spot.z - player.getZ()))
                > band.max() + HallucinationPlacement.OUTER_FROM_EDGE);
        if (found == null) {
            found = scan(level, player, bandAngles(band), min, max, random, spot -> outdoors.test(spot) && !canSee(level, player, spot));
        }
        if (found == null) {
            found = scan(level, player, round, min, max, random, spot -> outdoors.test(spot) && !canSee(level, player, spot));
        }
        if (found == null) {
            found = scan(level, player, round, min, max, random, spot -> !canSee(level, player, spot));
        }
        return found == null ? null : new Spot(found, false);
    }

    private static boolean isOpenSky(ClientLevel level, Vec3 feet) {

        BlockPos pos = BlockPos.containing(feet);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }

    private static List<Double> bandAngles(HallucinationPlacement.Band band) {

        List<Double> angles = new ArrayList<>();
        for (double angle = band.min(); angle <= band.max() + 1.0E-6; angle += SCAN_ANGLE_STEP) {
            angles.add(angle);
            angles.add(-angle);
        }
        return angles;
    }

    /**
     * Tries every (angle, distance) pair in a seeded random order; returns the first standable spot that passes
     * {@code accept} at block light ≤ {@link #MAX_BLOCK_LIGHT}, otherwise the first that passes at all.
     */
    private static @Nullable Vec3 scan(ClientLevel level, LocalPlayer player, List<Double> angles, double minDistance, double maxDistance,
                                       RandomSource random, Predicate<Vec3> accept) {

        List<double[]> candidates = new ArrayList<>();
        for (double angle : angles) {
            for (double distance = minDistance; distance <= maxDistance + 1.0E-6; distance += 1.0) {
                candidates.add(new double[]{angle, distance});
            }
        }
        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            double[] swap = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, swap);
        }
        Vec3 fallback = null;
        for (double[] candidate : candidates) {
            HallucinationPlacement.Offset offset = HallucinationPlacement.offset(player.getYRot(), candidate[0], candidate[1]);
            BlockPos column = BlockPos.containing(player.getX() + offset.dx(), player.getY(), player.getZ() + offset.dz());
            BlockPos feet = standableNear(level, column);
            if (feet == null) {
                continue;
            }
            Vec3 spot = Vec3.atBottomCenterOf(feet);
            if (!accept.test(spot)) {
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

    private static @Nullable Vec3 randomTries(ClientLevel level, LocalPlayer player, HallucinationPlacement.Band band, boolean reduced,
                                             RandomSource random) {

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
            if (!inBandAfterSnapping(player, band, spot) || !canSee(level, player, spot)) {
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
     * Standing the figure on a block centre moves it by up to ~0.7 blocks, which close in can push it several degrees
     * out of the band, so the final spot is checked again.
     */
    private static boolean inBandAfterSnapping(LocalPlayer player, HallucinationPlacement.Band band, Vec3 spot) {

        double angle = Math.abs(HallucinationPlacement.relativeAngle(player.getYRot(), spot.x - player.getX(), spot.z - player.getZ()));
        return angle >= band.min() && angle <= band.max();
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
        pending = null;
    }
}
