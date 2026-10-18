package io.github.mdw22.paranormalcraft.haunting;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Runs the later parts of multi-tick haunting effects (a chest lid closing, footsteps, knocks). Global rather than
 * per ghost, so a chest still closes if its ghost is dispersed or unloaded meanwhile. Server thread only.
 */
public final class HauntingScheduler {

    private record Task(ServerLevel level, long runAt, Runnable task) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private HauntingScheduler() {
    }

    public static void schedule(ServerLevel level, long delayTicks, Runnable task) {

        TASKS.add(new Task(level, level.getGameTime() + delayTicks, task));
    }

    public static void tick(MinecraftServer server) {

        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        Iterator<Task> tasks = TASKS.iterator();
        while (tasks.hasNext()) {
            Task task = tasks.next();
            if (task.level().getGameTime() >= task.runAt()) {
                due.add(task);
                tasks.remove();
            }
        }
        due.forEach(task -> task.task().run());
    }

    public static void clear() {

        TASKS.clear();
    }
}
