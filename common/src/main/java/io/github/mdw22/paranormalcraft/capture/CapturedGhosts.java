package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostNames;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Debug, scenes and tests: captured-ghost data made from a fresh Lingering Spirit, as if it had been caught, without
 * spawning anything.
 */
public final class CapturedGhosts {

    private CapturedGhosts() {
    }

    /**
     * A fresh Lingering Spirit homed at {@code home}, as a Filled Trap would hold it; null if it couldn't be made.
     */
    public static @Nullable CapturedGhost fresh(ServerLevel level, BlockPos home, String captor) {

        GhostEntity ghost = ModEntities.LINGERING_SPIRIT.get().create(level, EntitySpawnReason.COMMAND);
        if (ghost == null) {
            return null;
        }
        ghost.snapTo(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, 0, 0);
        ghost.setHome(home, home);
        ghost.setGhostName(GhostNames.random(level.getRandom()));
        CompoundTag tag;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
            ghost.save(output);
            tag = output.buildResult();
        }
        return new CapturedGhost(BuiltInRegistries.ENTITY_TYPE.getKey(ghost.getType()).toString(), tag, ghost.ghostName(),
                level.getGameTime(), captor, level.dimension().identifier().toString(), Optional.empty());
    }

    /**
     * A Filled Ghost Trap item holding a fresh Lingering Spirit.
     */
    public static ItemStack filledTrap(ServerLevel level, BlockPos home, String captor) {

        ItemStack stack = new ItemStack(ModItems.FILLED_GHOST_TRAP.get());
        CapturedGhost ghost = fresh(level, home, captor);
        if (ghost != null) {
            stack.set(ModDataComponents.CAPTURED_GHOST.get(), ghost);
        }
        return stack;
    }
}
