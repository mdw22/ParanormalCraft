package io.github.mdw22.paranormalcraft.ward;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-key rate limiting in game ticks, as plain logic so it can be unit tested: {@link #tryUse} succeeds at most once
 * per {@code interval} for each key (a lantern position, a mob, a player).
 */
public final class Cooldowns<K> {

    private final Map<K, Long> lastUse = new HashMap<>();

    /**
     * Whether {@code key} may act at {@code now}; if so, its cooldown starts.
     */
    public boolean tryUse(K key, long now, long interval) {

        Long last = this.lastUse.get(key);
        if (last != null && now - last < interval) {
            return false;
        }
        this.lastUse.put(key, now);
        return true;
    }

    public void clear() {

        this.lastUse.clear();
    }
}
