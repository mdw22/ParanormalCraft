package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to the player with a Containment Unit menu open (about twice a second): one row per stored ghost, for the
 * screen's ghost list. Charge and progress travel with the menu's own data slots.
 *
 * @param containerId the menu this is for; ignored if another menu is open
 */
public record ContainmentStatePayload(int containerId, List<Row> rows) implements CustomPacketPayload {

    /**
     * @param queuePosition 0 while processing, 1+ while waiting, -1 if not queued
     */
    public record Row(String name, String type, long storedTicks, int queuePosition) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Row::name,
                ByteBufCodecs.STRING_UTF8, Row::type,
                ByteBufCodecs.VAR_LONG, Row::storedTicks,
                ByteBufCodecs.VAR_INT, Row::queuePosition,
                Row::new);
    }

    public static final CustomPacketPayload.Type<ContainmentStatePayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("containment_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainmentStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ContainmentStatePayload::containerId,
            Row.STREAM_CODEC.apply(ByteBufCodecs.list()), ContainmentStatePayload::rows,
            ContainmentStatePayload::new);

    @Override
    public CustomPacketPayload.Type<ContainmentStatePayload> type() {

        return TYPE;
    }
}
