package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to one client: show this hallucination. The seed drives the client's placement, so it can be reproduced.
 * {@code report} asks the client to send back what happened ({@link FigureResultPayload}); {@code debug hallucinate}
 * sets it, natural hallucinations don't.
 */
public record HallucinationPayload(HallucinationKind kind, long seed, boolean report) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HallucinationPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("hallucination"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HallucinationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> HallucinationKind.values()[id], HallucinationKind::ordinal), HallucinationPayload::kind,
            ByteBufCodecs.VAR_LONG, HallucinationPayload::seed,
            ByteBufCodecs.BOOL, HallucinationPayload::report,
            HallucinationPayload::new);

    @Override
    public CustomPacketPayload.Type<HallucinationPayload> type() {

        return TYPE;
    }
}
