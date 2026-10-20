package io.github.mdw22.paranormalcraft.haunting;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.config.ServerConfig;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.tool.GhostEvents;
import io.github.mdw22.paranormalcraft.ward.Wards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Each ghost's haunting: while a player is within {@link #PLAYER_RANGE}, every {@code hauntingIntervalSeconds} it
 * picks a weighted-random action that has a target, is off its 60 s cooldown and, for griefing actions, is allowed by
 * {@code hauntingGriefing} and {@code mobGriefing}. Server only; state isn't saved (it simply reschedules after a load).
 */
public final class HauntingController {

    public static final double PLAYER_RANGE = Balance.Haunting.PLAYER_RANGE;
    private static final int HISTORY_SIZE = 8;

    /**
     * A completed action, for tests and debug output.
     */
    public record Record(HauntingAction action, long gameTime, String detail) {
    }

    private final GhostEntity ghost;
    private final HauntingSchedule schedule = new HauntingSchedule();
    private final Deque<Record> history = new ArrayDeque<>();

    public HauntingController(GhostEntity ghost) {

        this.ghost = ghost;
    }

    public HauntingSchedule schedule() {

        return this.schedule;
    }

    public List<Record> history() {

        return List.copyOf(this.history);
    }

    /**
     * Block-changing actions need both our config switch and the vanilla game rule.
     */
    public static boolean griefingAllowed(ServerLevel level) {

        return ModConfigs.server().hauntingGriefing() && level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    public void tick(ServerLevel level) {

        ServerConfig config = ModConfigs.server();
        if (!config.hauntingEnabled() || !playerNearby(level)) {
            return;
        }
        long now = level.getGameTime();
        if (!this.schedule.isScheduled()) {
            this.schedule.scheduleNext(now, rollInterval(config));
            return;
        }
        if (!this.schedule.isDue(now)) {
            return;
        }
        if (attempt(level, null, false).isEmpty()) {
            this.schedule.scheduleNext(now, rollInterval(config));
        }
    }

    /**
     * Debug and tests: act now, ignoring cooldowns but still obeying the griefing rule.
     *
     * @param action a specific action, or null for a weighted-random one
     */
    public Optional<Record> force(ServerLevel level, HauntingAction action) {

        return attempt(level, action, true);
    }

    private Optional<Record> attempt(ServerLevel level, HauntingAction only, boolean ignoreCooldowns) {

        long now = level.getGameTime();
        if (Wards.covers(level, this.ghost.position())) {
            ServerTrace.ghost("haunt_none", this.ghost, line -> line.addProperty("reason", "warded"));
            return Optional.empty();
        }
        HauntingTargets targets = HauntingTargets.find(level, this.ghost);
        List<HauntingPicker.Candidate> candidates = new ArrayList<>();
        for (HauntingAction action : HauntingAction.values()) {
            if (only == null || only == action) {
                candidates.add(new HauntingPicker.Candidate(action, targets.has(action), this.schedule.offCooldown(action, now)));
            }
        }
        boolean griefing = griefingAllowed(level);
        List<HauntingAction> eligible = HauntingPicker.eligibleActions(candidates, griefing, ignoreCooldowns);
        Optional<HauntingAction> picked = HauntingPicker.pick(eligible, bound -> this.ghost.getRandom().nextInt(bound));
        if (picked.isEmpty()) {
            // Gated: something had a target and was off cooldown, but griefing is off.
            boolean gated = !griefing && !HauntingPicker.eligibleActions(candidates, true, ignoreCooldowns).isEmpty();
            ServerTrace.ghost("haunt_none", this.ghost, line -> {
                line.addProperty("requested", only == null ? "any" : only.id());
                line.addProperty("forced", ignoreCooldowns);
                line.addProperty("gated", gated);
            });
            return Optional.empty();
        }
        HauntingAction action = picked.get();
        String detail = HauntingEffects.perform(action, targets, level, this.ghost);
        ServerTrace.ghost("haunt", this.ghost, line -> {
            line.addProperty("action", action.id());
            line.addProperty("target", String.valueOf(detail));
            line.addProperty("forced", ignoreCooldowns);
            line.addProperty("griefing", action.griefing());
            line.addProperty("done", detail != null);
        });
        if (detail == null) {
            return Optional.empty();
        }
        this.schedule.recordAction(action, now, rollInterval(ModConfigs.server()));
        Record record = new Record(action, now, detail);
        this.history.addFirst(record);
        while (this.history.size() > HISTORY_SIZE) {
            this.history.removeLast();
        }
        GhostEvents.onGhostActivity(this.ghost);
        Constants.LOG.debug("Ghost {} haunted: {} ({})", this.ghost.getUUID(), action.id(), detail);
        return Optional.of(record);
    }

    private boolean playerNearby(ServerLevel level) {

        return level.getNearestPlayer(this.ghost.getX(), this.ghost.getY(), this.ghost.getZ(), PLAYER_RANGE,
                player -> !player.isSpectator()) != null;
    }

    private long rollInterval(ServerConfig config) {

        int min = config.hauntingIntervalMinSeconds() * 20;
        int max = config.hauntingIntervalMaxSeconds() * 20;
        return ModConfigs.paced(min + this.ghost.getRandom().nextInt(max - min + 1));
    }
}
