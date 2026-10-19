package io.github.mdw22.paranormalcraft.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * What a Filled Ghost Trap holds: the ghost's type and its full saved entity data (so home, cold spot, Presence, name
 * and UUID all come back on release), plus who caught it, when, and where it came from. Stored on the filled trap
 * block entity and, when picked up, on the item ({@code paranormalcraft:captured_ghost}).
 *
 * @param entityType  the ghost's entity type id
 * @param entity      the ghost's full saved data (as {@code Entity.save} writes it, id included)
 * @param name        the ghost's generated name
 * @param captureTime game time of the capture
 * @param captor      the name of the player who caught it
 * @param dimension   the dimension it was caught in
 * @param structure   the structure its home is in, e.g. {@code abandoned_cabin}, if any
 */
public record CapturedGhost(String entityType, CompoundTag entity, String name, long captureTime, String captor, String dimension,
                            Optional<String> structure) {

    public static final Codec<CapturedGhost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("entity_type").forGetter(CapturedGhost::entityType),
            CompoundTag.CODEC.fieldOf("entity").forGetter(CapturedGhost::entity),
            Codec.STRING.fieldOf("name").forGetter(CapturedGhost::name),
            Codec.LONG.fieldOf("capture_time").forGetter(CapturedGhost::captureTime),
            Codec.STRING.fieldOf("captor").forGetter(CapturedGhost::captor),
            Codec.STRING.fieldOf("dimension").forGetter(CapturedGhost::dimension),
            Codec.STRING.optionalFieldOf("structure").forGetter(CapturedGhost::structure)
    ).apply(instance, CapturedGhost::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CapturedGhost> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
