package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the receiving player's {@code /paranormalcraft reveal} debug flag.
 */
public record RevealFlagPayload(boolean revealed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RevealFlagPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("reveal_flag"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RevealFlagPayload> STREAM_CODEC = ByteBufCodecs.BOOL
            .map(RevealFlagPayload::new, RevealFlagPayload::revealed)
            .cast();

    @Override
    public CustomPacketPayload.Type<RevealFlagPayload> type() {

        return TYPE;
    }
}
