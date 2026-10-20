package io.github.mdw22.paranormalcraft.playtest;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.ScriptRunner;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Playtest mode ({@code /pt}): a guided, click-through checklist for one player.
 * <ul>
 *     <li>Each step shows in chat with clickable buttons ({@code [Set up] [Pass] [Fail] [Note] [Skip] [Back]}) and on a
 *     boss bar ("Playtest 3/9: Wisp warning"), which stays clear of the tools' action-bar readouts.</li>
 *     <li>{@code [Set up]} runs the step's setup through the {@link ScriptRunner} as the player.</li>
 *     <li>An {@code auto_check} is evaluated every {@link #AUTO_CHECK_INTERVAL} ticks against the trace (buffered while a
 *     playtest runs) or the dump; when met, "✓ auto-check passed" shows. It's a hint: the player still judges.</li>
 *     <li>At the end (or {@code /pt stop}, or a logout) the results go to
 *     {@code <server dir>/paranormalcraft-playtests/<checklist>-<time>.md} and {@code latest.md}.</li>
 * </ul>
 */
public final class Playtests {

    public static final String RESULTS_DIR = "paranormalcraft-playtests";
    public static final String LATEST = "latest.md";
    private static final int AUTO_CHECK_INTERVAL = 10;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);
    private static final DateTimeFormatter HEADER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    /** One player's run. */
    public static final class Session {

        private final UUID player;
        private final String playerName;
        private final Checklist checklist;
        private final List<PlaytestResults.StepResult> results = new ArrayList<>();
        private final ServerBossEvent bar;
        private final LocalDateTime started = LocalDateTime.now();
        private int index;
        private long shownMillis;
        private long shownGameTime;
        private @Nullable Path resultsFile;

        Session(ServerPlayer player, Checklist checklist) {

            this.player = player.getUUID();
            this.playerName = player.getGameProfile().name();
            this.checklist = checklist;
            for (Checklist.Step step : checklist.steps()) {
                this.results.add(new PlaytestResults.StepResult(step.id(), step.title(), step.autoCheck() != null));
            }
            this.bar = new ServerBossEvent(UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            this.bar.addPlayer(player);
        }

        public Checklist checklist() {

            return this.checklist;
        }

        public int index() {

            return this.index;
        }

        public Checklist.Step step() {

            return this.checklist.steps().get(this.index);
        }

        public PlaytestResults.StepResult result() {

            return this.results.get(this.index);
        }

        public List<PlaytestResults.StepResult> results() {

            return List.copyOf(this.results);
        }

        public ServerBossEvent bar() {

            return this.bar;
        }

        public String playerName() {

            return this.playerName;
        }

        public @Nullable Path resultsFile() {

            return this.resultsFile;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** The last finished run per player, for tests and {@code /pt status} after the end. */
    private static final Map<UUID, Session> FINISHED = new HashMap<>();
    private static final Set<UUID> OFFERED = new HashSet<>();

    private Playtests() {
    }

    public static @Nullable Session session(ServerPlayer player) {

        return SESSIONS.get(player.getUUID());
    }

    /** For the debug dump: the runs in progress. */
    public static List<Session> active() {

        return List.copyOf(SESSIONS.values());
    }

    public static @Nullable Session lastFinished(ServerPlayer player) {

        return FINISHED.get(player.getUUID());
    }

    // ---- commands ----

    public static boolean start(ServerPlayer player, Checklist checklist) {

        Session old = SESSIONS.remove(player.getUUID());
        if (old != null) {
            end(player.level().getServer(), old, "replaced by /pt start " + checklist.name());
        }
        Session session = new Session(player, checklist);
        SESSIONS.put(player.getUUID(), session);
        ServerTrace.startBuffering(player.level().getServer());
        ServerTrace.event("playtest_start", line -> {
            line.addProperty("player", session.playerName);
            line.addProperty("checklist", checklist.name());
            line.addProperty("steps", checklist.steps().size());
        });
        player.sendSystemMessage(Component.literal("Playtest " + checklist.name() + ": " + checklist.title() + " (" + checklist.steps().size()
                + " steps). Results are saved when you finish.").withStyle(ChatFormatting.GOLD));
        show(player, session);
        return true;
    }

    public static void status(ServerPlayer player) {

        Session session = require(player);
        if (session != null) {
            show(player, session);
        }
    }

    /** Runs the current step's setup as {@code source} (the player's own command source when they click). */
    public static void setup(ServerPlayer player, CommandSourceStack source) {

        Session session = require(player);
        if (session == null) {
            return;
        }
        Checklist.Step step = session.step();
        if (step.setup().isEmpty()) {
            player.sendSystemMessage(Component.literal("This step has no setup.").withStyle(ChatFormatting.GRAY));
            return;
        }
        String id = step.id();
        ServerTrace.event("playtest_setup", line -> line.addProperty("step", id));
        ScriptRunner.start(source, "pt:" + session.checklist.name() + "/" + id, step.setup(), result -> {
            if (result.ok()) {
                player.sendSystemMessage(Component.literal("Set up: " + step.title()).withStyle(ChatFormatting.DARK_AQUA));
            } else {
                Constants.LOG.warn("Playtest setup {} failed at line {}: {}", id, result.line(), result.message());
                player.sendSystemMessage(Component.literal("Setup failed at line " + result.line() + ": " + result.message()).withStyle(ChatFormatting.RED));
            }
        });
    }

    public static void pass(ServerPlayer player) {

        mark(player, PlaytestResults.Status.PASS, null);
    }

    public static void fail(ServerPlayer player, @Nullable String note) {

        mark(player, PlaytestResults.Status.FAIL, note);
    }

    public static void skip(ServerPlayer player) {

        mark(player, PlaytestResults.Status.SKIP, null);
    }

    public static void note(ServerPlayer player, String note) {

        Session session = require(player);
        if (session != null) {
            session.result().addNote(note);
            player.sendSystemMessage(Component.literal("Noted on step " + (session.index + 1) + ".").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void back(ServerPlayer player) {

        Session session = require(player);
        if (session == null) {
            return;
        }
        if (session.index == 0) {
            player.sendSystemMessage(Component.literal("Already at the first step.").withStyle(ChatFormatting.GRAY));
            return;
        }
        session.result().addMillis(System.currentTimeMillis() - session.shownMillis);
        session.index--;
        session.result().setStatus(PlaytestResults.Status.PENDING);
        show(player, session);
    }

    public static void stop(ServerPlayer player) {

        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            player.sendSystemMessage(Component.literal("No playtest running.").withStyle(ChatFormatting.GRAY));
            return;
        }
        session.result().addMillis(System.currentTimeMillis() - session.shownMillis);
        end(player.level().getServer(), session, "stopped at step " + (session.index + 1) + " with /pt stop");
    }

    private static void mark(ServerPlayer player, PlaytestResults.Status status, @Nullable String note) {

        Session session = require(player);
        if (session == null) {
            return;
        }
        PlaytestResults.StepResult result = session.result();
        result.setStatus(status);
        if (note != null) {
            result.addNote(note);
        }
        result.addMillis(System.currentTimeMillis() - session.shownMillis);
        ServerTrace.event("playtest_result", line -> {
            line.addProperty("step", result.id());
            line.addProperty("result", status.name().toLowerCase(Locale.ROOT));
            line.addProperty("autoCheck", result.autoCheck().name().toLowerCase(Locale.ROOT));
        });
        if (session.index + 1 >= session.checklist.steps().size()) {
            SESSIONS.remove(player.getUUID());
            end(player.level().getServer(), session, "finished");
            return;
        }
        session.index++;
        show(player, session);
    }

    private static @Nullable Session require(ServerPlayer player) {

        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            player.sendSystemMessage(Component.literal("No playtest running. ").withStyle(ChatFormatting.GRAY)
                    .append(button("[List]", "/pt list", "Show the checklists", ChatFormatting.AQUA, false)));
        }
        return session;
    }

    // ---- display ----

    private static void show(ServerPlayer player, Session session) {

        Checklist.Step step = session.step();
        session.shownMillis = System.currentTimeMillis();
        session.shownGameTime = player.level().getGameTime();
        int number = session.index + 1;
        int total = session.checklist.steps().size();
        session.bar.setName(Component.literal("Playtest " + number + "/" + total + ": " + step.title()));
        session.bar.setProgress((float) session.index / total);
        session.bar.setColor(session.result().autoCheck() == PlaytestResults.AutoCheck.PASSED ? BossEvent.BossBarColor.GREEN : BossEvent.BossBarColor.PURPLE);
        player.sendSystemMessage(Component.literal("— Playtest " + number + "/" + total + ": " + step.title() + " —").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (String line : step.lines()) {
            player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.WHITE));
        }
        player.sendSystemMessage(buttons(session));
        ServerTrace.event("playtest_step", line -> {
            line.addProperty("step", step.id());
            line.addProperty("index", number);
            line.addProperty("of", total);
        });
    }

    /** The button row for the current step (plus the auto-check mark once it has passed). */
    public static MutableComponent buttons(Session session) {

        Checklist.Step step = session.step();
        MutableComponent row = Component.empty();
        if (!step.setup().isEmpty()) {
            row.append(button("[Set up]", "/pt setup", "Run: " + String.join("; ", step.setup()), ChatFormatting.AQUA, false)).append(" ");
        }
        row.append(button("[Pass]", "/pt pass", "It works and feels right", ChatFormatting.GREEN, false)).append(" ");
        row.append(button("[Fail]", "/pt fail ", "Type what went wrong, then press Enter", ChatFormatting.RED, true)).append(" ");
        row.append(button("[Note]", "/pt note ", "Add a note to this step", ChatFormatting.YELLOW, true)).append(" ");
        row.append(button("[Skip]", "/pt skip", "Skip this step", ChatFormatting.GRAY, false));
        if (session.index > 0) {
            row.append(" ").append(button("[Back]", "/pt back", "Back to the previous step", ChatFormatting.GRAY, false));
        }
        if (session.result().autoCheck() == PlaytestResults.AutoCheck.PASSED) {
            row.append(Component.literal("  ✓ auto-check passed").withStyle(ChatFormatting.GREEN));
        }
        return row;
    }

    /** A clickable chat button: runs {@code command}, or puts it in the chat box ({@code suggest}). */
    public static MutableComponent button(String label, String command, String hover, ChatFormatting colour, boolean suggest) {

        ClickEvent click = suggest ? new ClickEvent.SuggestCommand(command) : new ClickEvent.RunCommand(command);
        return Component.literal(label).withStyle(style -> style.withColor(colour).withClickEvent(click)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    // ---- ticking ----

    /** From {@code CommonEvents.onServerTick}: auto-checks. */
    public static void tick(MinecraftServer server) {

        if (SESSIONS.isEmpty() || server.getTickCount() % AUTO_CHECK_INTERVAL != 0) {
            return;
        }
        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.player);
            Checklist.Step step = session.step();
            if (player == null || step.autoCheck() == null || session.result().autoCheck() == PlaytestResults.AutoCheck.PASSED) {
                continue;
            }
            if (ScriptRunner.evaluate(step.autoCheck(), player.createCommandSourceStack(), session.shownGameTime) == null) {
                session.result().autoCheckPassed();
                session.bar.setColor(BossEvent.BossBarColor.GREEN);
                player.sendSystemMessage(buttons(session));
                ServerTrace.event("playtest_auto_check", line -> line.addProperty("step", step.id()));
            }
        }
    }

    // ---- the end ----

    private static void end(MinecraftServer server, Session session, String ending) {

        session.bar.removeAllPlayers();
        ServerTrace.stopBuffering();
        PlaytestResults.Header header = new PlaytestResults.Header(session.checklist.name(), session.checklist.title(),
                session.started.format(HEADER_TIME), BuildInfo.describe(server.getServerDirectory()), session.playerName, ending);
        String markdown = PlaytestResults.markdown(header, session.results);
        Path dir = server.getServerDirectory().resolve(RESULTS_DIR).toAbsolutePath().normalize();
        Path file = dir.resolve(session.checklist.name() + "-" + session.started.format(FILE_TIME) + ".md");
        try {
            Files.createDirectories(dir);
            Files.writeString(file, markdown, StandardCharsets.UTF_8);
            Files.copy(file, dir.resolve(LATEST), StandardCopyOption.REPLACE_EXISTING);
            session.resultsFile = file;
        } catch (IOException e) {
            Constants.LOG.error("Couldn't write playtest results to {}", file, e);
        }
        FINISHED.put(session.player, session);
        Path written = session.resultsFile;
        ServerTrace.event("playtest_end", line -> {
            line.addProperty("checklist", session.checklist.name());
            line.addProperty("ending", ending);
            line.addProperty("file", written == null ? "none" : written.toString());
        });
        Constants.LOG.info("Playtest {} {}: {}; results in {}", session.checklist.name(), ending, PlaytestResults.totals(session.results), written);
        ServerPlayer player = server.getPlayerList().getPlayer(session.player);
        if (player != null) {
            player.sendSystemMessage(Component.literal("Playtest " + session.checklist.name() + " " + ending + ": "
                    + PlaytestResults.totals(session.results)).withStyle(ChatFormatting.GOLD));
            for (int i = 0; i < session.results.size(); i++) {
                PlaytestResults.StepResult result = session.results.get(i);
                ChatFormatting colour = switch (result.status()) {
                    case PASS -> ChatFormatting.GREEN;
                    case FAIL -> ChatFormatting.RED;
                    default -> ChatFormatting.GRAY;
                };
                player.sendSystemMessage(Component.literal(" " + (i + 1) + ". " + result.title() + ": " + result.status().name().toLowerCase(Locale.ROOT)
                        + (result.notes().isEmpty() ? "" : " (" + String.join(" / ", result.notes()) + ")")).withStyle(colour));
            }
            if (written != null) {
                player.sendSystemMessage(Component.literal("Saved: " + written.getFileName() + " (and " + LATEST + ")").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    // ---- joining and leaving ----

    /**
     * In development runs, offers the newest checklist once per login to players who may use {@code /pt}.
     */
    public static void offer(ServerPlayer player) {

        if (!Services.PLATFORM.isDevelopmentEnvironment() || !ModConfigs.debugCommandsEnabled() || !OFFERED.add(player.getUUID())
                || !Commands.LEVEL_GAMEMASTERS.check(player.createCommandSourceStack().permissions())) {
            return;
        }
        String newest = Checklists.newest(Checklists.load(player.level().getServer()).checklists().keySet());
        if (newest != null) {
            player.sendSystemMessage(Component.literal("Playtest available: ").withStyle(ChatFormatting.GOLD)
                    .append(button("[Start " + newest + "]", "/pt start " + newest, "Start the " + newest + " checklist", ChatFormatting.AQUA, false))
                    .append(" ").append(button("[List]", "/pt list", "Show every checklist", ChatFormatting.GRAY, false)));
        }
    }

    /** A logout ends the player's run (results saved) and lets the offer show again next login. */
    public static void onLogout(ServerPlayer player) {

        OFFERED.remove(player.getUUID());
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.result().addMillis(System.currentTimeMillis() - session.shownMillis);
            end(player.level().getServer(), session, "stopped at step " + (session.index + 1) + " by a logout");
        }
    }

    public static void clear() {

        for (Session session : SESSIONS.values()) {
            session.bar.removeAllPlayers();
            ServerTrace.stopBuffering();
        }
        SESSIONS.clear();
        FINISHED.clear();
        OFFERED.clear();
    }
}
