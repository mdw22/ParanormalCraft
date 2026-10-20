package io.github.mdw22.paranormalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's Field Journal capture log, saved with the world ({@code data/paranormalcraft_capture_log.dat} in the
 * overworld's data folder). Vanilla saved data, so it works the same on both loaders. At most
 * {@link #MAX_ENTRIES} entries per player; the oldest go first.
 * <p>
 * The saved data type names a vanilla fixer ({@code SAVED_DATA_COMMAND_STORAGE}) because vanilla (Fabric) can't load
 * saved data without one; its fixes only touch command storage keys, so they leave this data alone.
 */
public final class CaptureLog extends SavedData {

    public static final int MAX_ENTRIES = Balance.Journal.CAPTURE_LOG_MAX;

    /**
     * One capture. {@code outcome} is {@code stored} (in a trap or a Containment Unit), {@code processed} or
     * {@code released}.
     */
    public record Entry(UUID ghost, String name, String type, long day, String outcome) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("ghost").forGetter(Entry::ghost),
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                Codec.STRING.fieldOf("type").forGetter(Entry::type),
                Codec.LONG.fieldOf("day").forGetter(Entry::day),
                Codec.STRING.fieldOf("outcome").forGetter(Entry::outcome)
        ).apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

        Entry withOutcome(String newOutcome) {

            return new Entry(this.ghost, this.name, this.type, this.day, newOutcome);
        }
    }

    private static final Codec<CaptureLog> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC.listOf())
            .xmap(CaptureLog::new, log -> log.entries)
            .fieldOf("players").codec();
    public static final SavedDataType<CaptureLog> TYPE = new SavedDataType<>(Constants.id("capture_log"), CaptureLog::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, List<Entry>> entries = new HashMap<>();

    private CaptureLog() {
    }

    private CaptureLog(Map<UUID, List<Entry>> loaded) {

        loaded.forEach((player, list) -> this.entries.put(player, new ArrayList<>(list)));
    }

    public static CaptureLog get(MinecraftServer server) {

        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Entry> entriesOf(UUID player) {

        return List.copyOf(this.entries.getOrDefault(player, List.of()));
    }

    public void add(UUID player, Entry entry) {

        List<Entry> list = this.entries.computeIfAbsent(player, key -> new ArrayList<>());
        list.add(entry);
        while (list.size() > MAX_ENTRIES) {
            list.removeFirst();
        }
        setDirty();
    }

    /**
     * Records what became of a ghost in every log that has it.
     */
    public void setOutcome(UUID ghost, String outcome) {

        for (List<Entry> list : this.entries.values()) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).ghost().equals(ghost)) {
                    list.set(i, list.get(i).withOutcome(outcome));
                    setDirty();
                }
            }
        }
    }

    public void reset(UUID player) {

        if (this.entries.remove(player) != null) {
            setDirty();
        }
    }
}
