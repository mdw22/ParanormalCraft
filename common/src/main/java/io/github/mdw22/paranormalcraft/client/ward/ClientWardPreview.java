package io.github.mdw22.paranormalcraft.client.ward;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.ward.WardLanternBlockEntity;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * While the local player holds a Ward Lantern, the boundaries of wards within {@link Balance.Ward#PREVIEW_RANGE} show as
 * a line of full-bright cyan motes on the ground at the radius edge (3 a block, 0.3 up), with a 3-high post every 4
 * blocks. Client only, on the stretch of edge within {@link #EDGE_RANGE} of the player, one round every
 * {@code PREVIEW_INTERVAL_TICKS}, capped at {@code PREVIEW_MAX_MOTES} a round.
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
        int budget = Balance.Ward.PREVIEW_MAX_MOTES;
        int spawned = 0;
        for (BlockPos lantern : LoadedPositions.WARD_LANTERNS_CLIENT.near(level, at, Balance.Ward.PREVIEW_RANGE)) {
            int radius = level.getBlockEntity(lantern) instanceof WardLanternBlockEntity ward ? ward.radius() : Balance.Ward.DEFAULT_RADIUS;
            int points = (int) Math.ceil(2 * Math.PI * radius * Balance.Ward.PREVIEW_MOTES_PER_BLOCK);
            int postEvery = Balance.Ward.PREVIEW_POST_SPACING * Balance.Ward.PREVIEW_MOTES_PER_BLOCK;
            for (int i = 0; i < points && spawned < budget; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = lantern.getX() + 0.5 + Math.cos(angle) * radius;
                double z = lantern.getZ() + 0.5 + Math.sin(angle) * radius;
                if ((x - at.x) * (x - at.x) + (z - at.z) * (z - at.z) > EDGE_RANGE * EDGE_RANGE) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                double y = (Math.abs(top - lantern.getY()) <= 8 ? top : lantern.getY()) + Balance.Ward.MOTE_LIFT;
                level.addParticle(ParticleTypes.GLOW, x, y, z, 0, 0.005, 0);
                spawned++;
                if (i % postEvery == 0) {
                    for (int h = 1; h < Balance.Ward.PREVIEW_POST_HEIGHT && spawned < budget; h++) {
                        level.addParticle(ParticleTypes.GLOW, x, y + h, z, 0, 0.005, 0);
                        spawned++;
                    }
                }
            }
        }
        if (spawned != lastRound) {
            int motes = spawned;
            ClientTrace.event("ward_preview_round", line -> line.addProperty("motes", motes));
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
