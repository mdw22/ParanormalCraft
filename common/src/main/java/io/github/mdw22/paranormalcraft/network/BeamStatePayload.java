package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to every player in a level, each tick while any Tether beam is active (and once more when the last one ends):
 * the active beams. Clients draw each from its owner's wand to the latched ghost, or to the free end point.
 */
public record BeamStatePayload(List<Beam> beams) implements CustomPacketPayload {

    /**
     * @param owner  the firing player's entity id
     * @param target the latched ghost's entity id, or -1
     */
    public record Beam(int owner, int target, double endX, double endY, double endZ) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Beam> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Beam::owner,
                ByteBufCodecs.VAR_INT, Beam::target,
                ByteBufCodecs.DOUBLE, Beam::endX,
                ByteBufCodecs.DOUBLE, Beam::endY,
                ByteBufCodecs.DOUBLE, Beam::endZ,
                Beam::new);
    }

    public static final CustomPacketPayload.Type<BeamStatePayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("beam_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeamStatePayload> STREAM_CODEC = Beam.STREAM_CODEC
            .apply(ByteBufCodecs.list(64))
            .map(BeamStatePayload::new, BeamStatePayload::beams);

    @Override
    public CustomPacketPayload.Type<BeamStatePayload> type() {

        return TYPE;
    }
}
