package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ward Lantern coverage. A loaded lantern protects everything within {@code wardLanternRadius} (server config) of
 * it: no natural monster spawns, no haunting actions by ghosts inside, no shadow figures for players inside. Ghosts
 * themselves can still be inside a ward, harmless but catchable. Positions come from
 * {@link LoadedPositions#WARD_LANTERNS}, so no check ever scans for blocks.
 */
public final class Wards {

    private Wards() {
    }

    public static boolean covers(Level level, Vec3 pos) {

        return LoadedPositions.WARD_LANTERNS.anyWithin(level, pos, ModConfigs.server().wardLanternRadius());
    }

    /**
     * Whether a natural spawn of {@code category} at {@code pos} is blocked: monsters inside a ward. Called from the
     * natural spawner's position check (see {@code NaturalSpawnerMixin}).
     */
    public static boolean blocksNaturalSpawn(Level level, MobCategory category, BlockPos pos) {

        return category == MobCategory.MONSTER && covers(level, Vec3.atBottomCenterOf(pos));
    }
}
