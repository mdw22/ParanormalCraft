package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Only registers its Ward Lantern in {@link LoadedPositions#WARD_LANTERNS} while loaded (placed or chunk loaded) and
 * removes it when broken or unloaded. No ticking.
 */
public class WardLanternBlockEntity extends BlockEntity {

    public WardLanternBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlocks.WARD_LANTERN_ENTITY.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {

        super.setLevel(level);
        if (!level.isClientSide()) {
            LoadedPositions.WARD_LANTERNS.add(level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {

        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            LoadedPositions.WARD_LANTERNS.remove(this.level, this.worldPosition);
        }
    }

    @Override
    public void clearRemoved() {

        super.clearRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            LoadedPositions.WARD_LANTERNS.add(this.level, this.worldPosition);
        }
    }
}
