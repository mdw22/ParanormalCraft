package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Registers its Ward Lantern in {@link LoadedPositions#WARD_LANTERNS} while loaded (placed or chunk loaded) and removes
 * it when broken or unloaded; clients keep their own registry ({@link LoadedPositions#WARD_LANTERNS_CLIENT}) for the
 * boundary preview. The server's ward radius is synced to clients with the block entity's update tag. No ticking.
 */
public class WardLanternBlockEntity extends BlockEntity {

    private int radius = Balance.Ward.DEFAULT_RADIUS;

    public WardLanternBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlocks.WARD_LANTERN_ENTITY.get(), pos, state);
    }

    /** The ward's radius (on the client, the server's value). */
    public int radius() {

        return this.radius;
    }

    private static LoadedPositions registry(Level level) {

        return level.isClientSide() ? LoadedPositions.WARD_LANTERNS_CLIENT : LoadedPositions.WARD_LANTERNS;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {

        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Radius", ModConfigs.server().wardLanternRadius());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        this.radius = input.getIntOr("Radius", this.radius);
    }

    @Override
    public void setLevel(Level level) {

        super.setLevel(level);
        if (!level.isClientSide()) {
            this.radius = ModConfigs.server().wardLanternRadius();
        }
        registry(level).add(level, this.worldPosition);
    }

    @Override
    public void setRemoved() {

        super.setRemoved();
        if (this.level != null) {
            registry(this.level).remove(this.level, this.worldPosition);
        }
    }

    @Override
    public void clearRemoved() {

        super.clearRemoved();
        if (this.level != null) {
            registry(this.level).add(this.level, this.worldPosition);
        }
    }
}
