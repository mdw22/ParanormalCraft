package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to client: a debug-toolkit request for the receiving client, e.g. {@code screenshot <name> <w> <h>},
 * {@code trace_start}, {@code trace_stop}, {@code trace_mark <label>}, {@code trace_snapshot}, {@code config_set <key>
 * <value>} or {@code config_reload}. Only sent by {@code /paranormalcraft debug …}.
 */
public record DebugClientPayload(String action, List<String> args) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DebugClientPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("debug_client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DebugClientPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DebugClientPayload::action,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), DebugClientPayload::args,
            DebugClientPayload::new);

    public static DebugClientPayload of(String action, String... args) {

        return new DebugClientPayload(action, List.of(args));
    }

    public String arg(int index, String fallback) {

        return index < this.args.size() ? this.args.get(index) : fallback;
    }

    @Override
    public CustomPacketPayload.Type<DebugClientPayload> type() {

        return TYPE;
    }
}
