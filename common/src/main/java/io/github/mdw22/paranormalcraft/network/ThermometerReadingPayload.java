package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the receiving player's thermometer reading in °C. The client converts units when it displays it.
 */
public record ThermometerReadingPayload(float celsius) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ThermometerReadingPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("thermometer_reading"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThermometerReadingPayload> STREAM_CODEC = ByteBufCodecs.FLOAT
            .map(ThermometerReadingPayload::new, ThermometerReadingPayload::celsius)
            .cast();

    @Override
    public CustomPacketPayload.Type<ThermometerReadingPayload> type() {

        return TYPE;
    }
}
