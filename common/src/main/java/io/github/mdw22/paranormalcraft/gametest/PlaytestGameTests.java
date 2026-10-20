package io.github.mdw22.paranormalcraft.gametest;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import io.github.mdw22.paranormalcraft.playtest.Checklist;
import io.github.mdw22.paranormalcraft.playtest.Checklists;
import io.github.mdw22.paranormalcraft.playtest.Playtests;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GameTests for playtest mode: a {@code /pt} run through the 3-step {@code selftest} checklist (setup runs and its
 * auto-check passes, pass, fail with a note, skip) with the results file checked; and every shipped checklist's setup
 * commands parse against the real command tree, so a checklist can't rot when commands change. Own batch
 * ({@code playtest}): the trace buffer and the time of day are shared.
 */
public final class PlaytestGameTests {

    private PlaytestGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("playtest_selftest_run", PlaytestGameTests::selftestRun);
        RegistrationHelper.testFunction("playtest_checklists_setup_parses", PlaytestGameTests::setupParses);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    // TODO(26.3): makeMockServerPlayerInLevel is deprecated; switch once a replacement exists.
    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(20, 1, 20)));
        player.teleportTo(at.x, at.y, at.z);
        return player;
    }

    /** Runs {@code /<command>} as the player, with op rights (mock players have none). */
    private static void run(GameTestHelper helper, ServerPlayer player, String command) {

        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER);
        server.getCommands().performPrefixedCommand(source, command);
    }

    private static void selftestRun(GameTestHelper helper) {

        ServerPlayer player = player(helper);
        run(helper, player, "pt start selftest");
        Playtests.Session session = Playtests.session(player);
        check(helper, session != null && session.checklist().name().equals("selftest"), "/pt start selftest didn't start a run");
        check(helper, session.bar().getPlayers().contains(player), "no boss bar line for the player");
        check(helper, session.bar().getName().getString().equals("Playtest 1/3: Set up and pass"), "boss bar says '%s'", session.bar().getName().getString());
        run(helper, player, "pt setup");
        helper.runAfterDelay(30, () -> {
            try {
                finish(helper, player, session);
            } finally {
                Playtests.stop(player);
                player.level().getServer().getPlayerList().remove(player);
            }
            helper.succeed();
        });
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, Playtests.Session session) {

        {
            check(helper, session.results().getFirst().autoCheck().name().equals("PASSED"), "the setup's auto-check didn't pass");
            run(helper, player, "pt pass");
            check(helper, session.index() == 1, "pass didn't move on (step %d)", session.index() + 1);
            run(helper, player, "pt back");
            check(helper, session.index() == 0 && session.results().getFirst().status().name().equals("PENDING"), "back didn't return to step 1");
            run(helper, player, "pt pass");
            run(helper, player, "pt fail the note text");
            run(helper, player, "pt skip");
            check(helper, Playtests.session(player) == null, "the run didn't end after the last step");
            Playtests.Session done = Playtests.lastFinished(player);
            check(helper, done == session && done.resultsFile() != null, "no results file");
            check(helper, session.bar().getPlayers().isEmpty(), "the boss bar is still shown");
            try {
                Path file = done.resultsFile();
                String markdown = Files.readString(file, StandardCharsets.UTF_8);
                String latest = Files.readString(file.resolveSibling(Playtests.LATEST), StandardCharsets.UTF_8);
                check(helper, markdown.equals(latest), "latest.md differs from the run's file");
                check(helper, file.getFileName().toString().startsWith("selftest-") && file.toString().endsWith(".md"), "file name %s", file.getFileName());
                check(helper, markdown.startsWith("# Playtest: selftest — Playtest mode self-test"), "header: %s", markdown.lines().findFirst().orElse(""));
                check(helper, markdown.contains("- **Player:** test-mock-player"), "no player line");
                check(helper, markdown.contains("- **Run:** finished"), "no run line");
                check(helper, markdown.contains("- **Result:** 1 pass, 1 fail, 1 skip, 0 not reached (3 steps)"), "totals wrong:\n%s", markdown);
                check(helper, markdown.contains("| 1 | Set up and pass (`setup_runs`) | pass | ✓ passed |"), "row 1 wrong:\n%s", markdown);
                check(helper, markdown.contains("| 2 | Fail with a note (`fail_with_note`) | **FAIL** | — |") && markdown.contains("| the note text |"),
                        "row 2 wrong:\n%s", markdown);
                check(helper, markdown.contains("| 3 | Skip (`skip`) | skip | — |"), "row 3 wrong:\n%s", markdown);
            } catch (IOException e) {
                throw helper.assertionException(Component.literal("couldn't read the results: " + e));
            }
        }
    }

    private static void setupParses(GameTestHelper helper) {

        MinecraftServer server = helper.getLevel().getServer();
        Checklists.Loaded loaded = Checklists.load(server);
        check(helper, loaded.invalid().isEmpty(), "invalid checklists: %s", loaded.invalid().stream().map(Throwable::getMessage).toList());
        check(helper, loaded.checklists().containsKey("update10") && loaded.checklists().containsKey("selftest"), "shipped checklists missing: %s",
                loaded.checklists().keySet());
        CommandSourceStack source = server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER);
        List<String> broken = new ArrayList<>();
        int checked = 0;
        for (Checklist checklist : loaded.checklists().values()) {
            for (Checklist.Step step : checklist.steps()) {
                for (String line : step.setup()) {
                    String command = line.strip().startsWith("/") ? line.strip().substring(1) : line.strip();
                    if (command.isEmpty() || command.startsWith("#") || command.toLowerCase(Locale.ROOT).startsWith("wait ")) {
                        continue;
                    }
                    ParseResults<CommandSourceStack> parse = server.getCommands().getDispatcher().parse(command, source);
                    CommandContextBuilder<CommandSourceStack> last = parse.getContext().getLastChild();
                    if (parse.getReader().canRead() || !parse.getExceptions().isEmpty() || last.getCommand() == null) {
                        broken.add(checklist.name() + "/" + step.id() + ": '" + command + "'");
                    }
                    checked++;
                }
            }
        }
        check(helper, broken.isEmpty(), "setup commands that don't parse: %s", broken);
        check(helper, checked > 20, "only %d setup commands checked", checked);
        helper.succeed();
    }
}
