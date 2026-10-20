package io.github.mdw22.paranormalcraft.client.ward;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.ward.WardEffects;
import io.github.mdw22.paranormalcraft.ward.WardLanternBlockEntity;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * While the local player holds a Ward Lantern, the boundaries of wards within {@link Balance.Ward#PREVIEW_RANGE} show as
 * faint, sparse motes on the ground at the radius edge. Client only and cheap: a few motes per ward every few ticks,
 * and only on the stretch of edge within {@link #EDGE_RANGE} of the player.
 */
public final class ClientWardPreview {

    private static final int INTERVAL_TICKS = Balance.Ward.PREVIEW_INTERVAL_TICKS;
    private static final int MOTES_PER_WARD = Balance.Ward.PREVIEW_MOTES;
    /** Only edge points this close to the player get motes. */
    private static final double EDGE_RANGE = Balance.Ward.PREVIEW_EDGE_RANGE;
    private static boolean wasShowing;

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
        RandomSource random = level.getRandom();
        Vec3 at = player.position();
        for (BlockPos lantern : LoadedPositions.WARD_LANTERNS_CLIENT.near(level, at, Balance.Ward.PREVIEW_RANGE)) {
            int radius = level.getBlockEntity(lantern) instanceof WardLanternBlockEntity ward ? ward.radius() : Balance.Ward.DEFAULT_RADIUS;
            for (int i = 0; i < MOTES_PER_WARD; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double x = lantern.getX() + 0.5 + Math.cos(angle) * radius;
                double z = lantern.getZ() + 0.5 + Math.sin(angle) * radius;
                if ((x - at.x) * (x - at.x) + (z - at.z) * (z - at.z) > EDGE_RANGE * EDGE_RANGE) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                double y = Math.abs(top - lantern.getY()) <= 8 ? top : lantern.getY();
                level.addParticle(i % 2 == 0 ? ParticleTypes.GLOW : WardEffects.MOTE, x, y + 0.15, z, 0, 0.01, 0);
            }
        }
    }

    public static void reset() {

        LoadedPositions.WARD_LANTERNS_CLIENT.clear();
        wasShowing = false;
    }
}
