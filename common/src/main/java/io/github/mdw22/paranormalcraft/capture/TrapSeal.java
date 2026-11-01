package io.github.mdw22.paranormalcraft.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A Filled Trap's seal: the game time it was sealed, the integrity it had then (%), and how many ticks a full seal
 * lasts. Integrity is worked out from these and the current game time ({@link TrapIntegrity}), so a trap decays in a
 * chest, an inventory or an unloaded chunk without anything ticking it. Stored as
 * {@code paranormalcraft:trap_seal} on the item and on the placed trap.
 */
public record TrapSeal(long sealedAt, float startIntegrity, int lifetimeTicks) {

    public static final Codec<TrapSeal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("sealed_at").forGetter(TrapSeal::sealedAt),
            Codec.FLOAT.fieldOf("start_integrity").forGetter(TrapSeal::startIntegrity),
            Codec.INT.fieldOf("lifetime_ticks").forGetter(TrapSeal::lifetimeTicks)
    ).apply(instance, TrapSeal::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrapSeal> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TrapSeal::sealedAt,
            ByteBufCodecs.FLOAT, TrapSeal::startIntegrity,
            ByteBufCodecs.VAR_INT, TrapSeal::lifetimeTicks,
            TrapSeal::new);
}
