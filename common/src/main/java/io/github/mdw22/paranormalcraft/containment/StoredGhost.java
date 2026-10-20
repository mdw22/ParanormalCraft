package io.github.mdw22.paranormalcraft.containment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * A ghost held in a Containment Unit: the same data a Filled Trap holds ({@link CapturedGhost}), plus when it went in
 * and, if it is queued for processing, who queued it.
 *
 * @param ghost     the captured ghost, full entity data included
 * @param storedAt  game time it was loaded
 * @param queuedBy  the player who chose Process, while it waits or processes
 */
public record StoredGhost(CapturedGhost ghost, long storedAt, Optional<UUID> queuedBy) {

    public static final Codec<StoredGhost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CapturedGhost.CODEC.fieldOf("ghost").forGetter(StoredGhost::ghost),
            Codec.LONG.fieldOf("stored_at").forGetter(StoredGhost::storedAt),
            UUIDUtil.CODEC.optionalFieldOf("queued_by").forGetter(StoredGhost::queuedBy)
    ).apply(instance, StoredGhost::new));

    /**
     * The ghost's entity UUID, from its saved data (a random one if missing).
     */
    public UUID id() {

        return this.ghost.entity().read("UUID", UUIDUtil.CODEC).orElseGet(() -> UUID.nameUUIDFromBytes(this.ghost.name().getBytes()));
    }

    public StoredGhost queued(UUID player) {

        return new StoredGhost(this.ghost, this.storedAt, Optional.of(player));
    }
}
