package io.github.mdw22.paranormalcraft.client.ward;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.ward.WardLanternBlockEntity;
import io.github.mdw22.paranormalcraft.ward.WardRing;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * While the local player holds a Ward Lantern, the boundaries of wards within {@link Balance.Ward#PREVIEW_RANGE} show as
 * a line of full-bright cyan motes on the ground at the radius edge (3 a block, 0.3 up), with a 3-high post every 4
 * blocks. Client only, on the stretch of edge within {@link #EDGE_RANGE} of the player, one round every
 * {@code PREVIEW_INTERVAL_TICKS}, at most {@code PREVIEW_MAX_MOTES} a round (a round over that gets sparser, see
 * {@link WardRing}).
 */
public final class ClientWardPreview {

    private static final int INTERVAL_TICKS = Balance.Ward.PREVIEW_INTERVAL_TICKS;
    /** Only edge points this close to the player get motes. */
    private static final double EDGE_RANGE = Balance.Ward.PREVIEW_EDGE_RANGE;
    private static boolean wasShowing;
    private static int lastRound;

    private ClientWardPreview() {
    }

    public static void tick(Minecraft minecraft) {

        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        boolean holding = player.getMainHandItem().is(ModItems.WARD_LANTERN.get()) || player.getOffhandItem().is(ModItems.WARD_LANTERN.get());
        if (holding != wasShowing) {
            wasShowing = holding;
            ClientTrace.event("ward_preview", line -> line.addProperty("showing", holding));
        }
        if (!holding || player.tickCount % INTERVAL_TICKS != 0) {
            return;
        }
        Vec3 at = player.position();
        List<BlockPos> lanterns = List.copyOf(LoadedPositions.WARD_LANTERNS_CLIENT.near(level, at, Balance.Ward.PREVIEW_RANGE));
        int budget = Balance.Ward.PREVIEW_MAX_MOTES;
        int spawned = 0;
        double widestGap = 0.0;
        for (int w = 0; w < lanterns.size(); w++) {
            BlockPos lantern = lanterns.get(w);
            int radius = level.getBlockEntity(lantern) instanceof WardLanternBlockEntity ward ? ward.radius() : Balance.Ward.DEFAULT_RADIUS;
            double centreX = lantern.getX() + 0.5;
            double centreZ = lantern.getZ() + 0.5;
            // An even share of what's left, so no ward loses the end of its circle to another.
            List<WardRing.Point> points = WardRing.previewRound(centreX, centreZ, radius, at.x, at.z, EDGE_RANGE, (budget - spawned) / (lanterns.size() - w));
            widestGap = Math.max(widestGap, WardRing.widestGap(points, centreX, centreZ, radius, at.x, at.z, EDGE_RANGE));
            for (WardRing.Point point : points) {
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(point.x()), (int) Math.floor(point.z()));
                double y = (Math.abs(top - lantern.getY()) <= 8 ? top : lantern.getY()) + Balance.Ward.MOTE_LIFT;
                level.addParticle(ParticleTypes.GLOW, point.x(), y, point.z(), 0, 0.005, 0);
                spawned++;
                if (point.post()) {
                    for (int h = 1; h < Balance.Ward.PREVIEW_POST_HEIGHT; h++) {
                        level.addParticle(ParticleTypes.GLOW, point.x(), y + h, point.z(), 0, 0.005, 0);
                        spawned++;
                    }
                }
            }
        }
        if (spawned != lastRound) {
            int motes = spawned;
            double gap = Math.round(widestGap * 100.0) / 100.0;
            ClientTrace.event("ward_preview_round", line -> {
                line.addProperty("motes", motes);
                line.addProperty("gap", gap);
            });
        }
        lastRound = spawned;
    }

    /** Motes spawned in the last preview round (for the trace and the particle budget). */
    public static int lastRound() {

        return lastRound;
    }

    public static void reset() {

        LoadedPositions.WARD_LANTERNS_CLIENT.clear();
        wasShowing = false;
    }
}
