package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what became of a reported hallucination: {@code placed (visible)}, {@code placed (sound-first)}
 * or {@code skipped: <reason>}. The server logs it, tells the player and traces it as {@code figure_result}.
 */
public record FigureResultPayload(String result) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FigureResultPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("figure_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FigureResultPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(FigureResultPayload::new, FigureResultPayload::result)
            .cast();

    @Override
    public CustomPacketPayload.Type<FigureResultPayload> type() {

        return TYPE;
    }
}
