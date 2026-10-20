package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: whether a player (by entity id) is phased by the Spectral Step. Sent to the player and everyone
 * near them when it starts and ends, and again every half second while it lasts for anyone who came into range. The
 * phased player's own client needs it to predict movement through blocks; the others to draw them translucent.
 */
public record PhaseStatePayload(int entityId, boolean phased) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PhaseStatePayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("phase_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhaseStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PhaseStatePayload::entityId,
            ByteBufCodecs.BOOL, PhaseStatePayload::phased,
            PhaseStatePayload::new);

    @Override
    public CustomPacketPayload.Type<PhaseStatePayload> type() {

        return TYPE;
    }
}
