package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Presence;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bringing a captured ghost back into the world: a release (calm, its old home) or an escape from a decayed trap or a
 * Containment breach (full Presence, Agitated, homed where it got out, and attached to the nearest player).
 */
public final class LooseGhosts {

    private static final int TICKS_PER_MINUTE = 1200;

    private LooseGhosts() {
    }

    /**
     * Recreates {@code captured} at {@code at} with full Presence. Null if its data doesn't load as a ghost.
     */
    public static @Nullable GhostEntity recreate(ServerLevel level, CapturedGhost captured, Vec3 at) {

        CompoundTag data = captured.entity().copy();
        data.putFloat("Presence", Presence.MAX);
        Entity entity;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            entity = EntityType.loadEntityRecursive(TagValueInput.create(reporter, level.registryAccess(), data), level, EntitySpawnReason.LOAD, loaded -> loaded);
        }
        if (!(entity instanceof GhostEntity ghost)) {
            return null;
        }
        ghost.snapTo(at.x, at.y, at.z, ghost.getYRot(), 0.0F);
        level.addFreshEntity(ghost);
        return ghost;
    }

    /**
     * A ghost gets loose: recreated at {@code at}, Agitated for the configured time, homed at {@code home}, and
     * attached to the nearest player within range.
     */
    public static @Nullable GhostEntity escape(ServerLevel level, CapturedGhost captured, Vec3 at, BlockPos home, String cause) {

        GhostEntity ghost = recreate(level, captured, at);
        if (ghost == null) {
            return null;
        }
        ghost.setHome(level, home);
        ghost.agitate(ModConfigs.server().agitatedMinutes() * TICKS_PER_MINUTE);
        ghost.attachment().attachToNearest(level, cause);
        return ghost;
    }
}
