package io.github.mdw22.paranormalcraft.fabric.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.mdw22.paranormalcraft.client.ClientToolState;
import io.github.mdw22.paranormalcraft.client.hallucination.ClientHallucinations;
import io.github.mdw22.paranormalcraft.entity.ShadowFigureEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationPlacement;
import io.github.mdw22.paranormalcraft.tool.ThermometerReadings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import io.github.mdw22.paranormalcraft.registry.ModCreativeTabs;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Client GameTests for the things that have to be seen. One superflat world, six checks, screenshots saved to
 * {@code fabric/build/run/clientGameTest/screenshots}:
 * <ol>
 *     <li>a ghost is hidden without goggles and shown with them ({@code canSee} asserted on the client);</li>
 *     <li>the EMF Reader model at level 1 and at level 5;</li>
 *     <li>the thermometer's action bar while standing on a cold spot;</li>
 *     <li>a manifest fading out step by step for a player without goggles (from the client trace), with a
 *     {@code debug screenshot} of the middle of the fade;</li>
 *     <li>a shadow figure from {@code debug hallucinate}, on screen when it appears, whose presence sound plays at its
 *     position every 20–30 ticks (from the client trace) and stops once it is looked at and vanishes;</li>
 *     <li>the Paranormal Craft creative tab and its four items.</li>
 * </ol>
 * No golden-image comparison yet: the art is placeholder.
 */
public class ToolsAndVisibilityClientGameTest implements FabricClientGameTest {

    private static final String GHOST = "paranormalcraft:lingering_spirit";
    private static final int READING_WAIT_TICKS = 30;

    @Override
    public void runTest(ClientGameTestContext context) {

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            TestServerContext server = world.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule advance_weather false");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("tp @a 0.5 -60 0.5 180 10");
            context.waitTicks(20);

            ghostVisibility(context, server);
            emfLevels(context, server);
            thermometerAtColdSpot(context, server);
            manifestFadeOut(context, server);
            shadowFigure(context, server);
            creativeTab(context, server);
        }
    }

    private static void ghostVisibility(ClientGameTestContext context, TestServerContext server) {

        server.runCommand("summon " + GHOST + " 0.5 -59.5 -2.5 {NoAI:1b}");
        context.waitTicks(20);
        boolean seenWithout = context.computeOnClient(client -> canLocalPlayerSee(client));
        check(!seenWithout, "ghost visible without goggles");
        context.takeScreenshot("01-ghost-hidden-without-goggles");

        server.runCommand("item replace entity @a armor.head with paranormalcraft:spectral_goggles");
        context.waitTicks(10);
        boolean seenWith = context.computeOnClient(client -> canLocalPlayerSee(client));
        check(seenWith, "ghost hidden with goggles on");
        context.takeScreenshot("02-ghost-shown-with-goggles");

        server.runCommand("item replace entity @a armor.head with air");
        server.runCommand("kill @e[type=" + GHOST + "]");
        context.waitTicks(5);
    }

    /**
     * canSee for every ghost near the local player; also checks the client-side invisibility hook agrees.
     */
    private static boolean canLocalPlayerSee(Minecraft client) {

        List<GhostEntity> ghosts = client.level.getEntitiesOfClass(GhostEntity.class, new AABB(client.player.blockPosition()).inflate(8));
        check(!ghosts.isEmpty(), "no ghost reached the client");
        boolean all = true;
        for (GhostEntity ghost : ghosts) {
            boolean sees = GhostVisibility.canSee(client.player, ghost);
            check(sees == GhostVisibility.canClientSee(ghost), "canSee and canClientSee disagree");
            check(ghost.isInvisible() == !sees, "isInvisible() doesn't match canSee");
            check(ghost.isPickable() == sees, "isPickable() doesn't match canSee");
            all &= sees;
        }
        return all;
    }

    private static void emfLevels(ClientGameTestContext context, TestServerContext server) {

        server.runCommand("item replace entity @a weapon.mainhand with paranormalcraft:emf_reader");
        context.waitTicks(READING_WAIT_TICKS);
        int baseline = context.computeOnClient(client -> ClientToolState.emfLevel());
        check(baseline == 1, "EMF baseline is " + baseline + ", expected 1");
        context.takeScreenshot("03-emf-level-1");

        server.runCommand("summon " + GHOST + " 0.5 -59.5 -2.5 {NoAI:1b}");
        context.waitTicks(READING_WAIT_TICKS);
        int close = context.computeOnClient(client -> ClientToolState.emfLevel());
        check(close == 5, "EMF next to a ghost is " + close + ", expected 5");
        context.takeScreenshot("04-emf-level-5");

        server.runCommand("kill @e[type=" + GHOST + "]");
        server.runCommand("item replace entity @a weapon.mainhand with air");
        context.waitTicks(5);
    }

    /**
     * Open sky everywhere, so the ghost's cold spot falls back to its home, which is the block the player stands in.
     */
    private static void thermometerAtColdSpot(ClientGameTestContext context, TestServerContext server) {

        server.runCommand("summon " + GHOST + " 0.5 -60 0.5 {NoAI:1b}");
        context.waitTicks(5);
        server.runCommand("item replace entity @a weapon.mainhand with paranormalcraft:spirit_thermometer");
        context.waitTicks(READING_WAIT_TICKS);
        double expected = server.computeOnServer(s -> {
            var level = s.overworld();
            var player = s.getPlayerList().getPlayers().getFirst();
            check(ThermometerReadings.coldSpotOffset(level, player.position()) == -10.0, "player isn't on the cold spot centre");
            return ThermometerReadings.readWithoutNoise(level, player.position());
        });
        double shown = context.computeOnClient(client -> ClientToolState.lastCelsius());
        String text = context.computeOnClient(client -> ClientToolState.lastActionBar().getString());
        check(Math.abs(shown - expected) <= 0.3 + 1.0E-4, String.format(Locale.ROOT, "reading %.2f is not within noise of %.2f", shown, expected));
        check(text.equals(String.format(Locale.ROOT, "%.1f°C", shown)), "action bar shows '" + text + "'");
        context.takeScreenshot("05-thermometer-action-bar-at-cold-spot");
    }

    /**
     * {@code debug hallucinate} shows a client-only figure off to the side; looking at it makes it vanish.
     */
    private static void shadowFigure(ClientGameTestContext context, TestServerContext server) {

        server.runCommand("kill @e[type=" + GHOST + "]");
        server.runCommand("item replace entity @a weapon.mainhand with air");
        server.runCommand("kill @e[type=item]");
        server.runCommand("tp @a 0.5 -60 0.5 180 0");
        context.waitTicks(10);
        server.runCommand("execute as @p run paranormalcraft debug trace start");
        context.waitTicks(3);
        server.runCommand("paranormalcraft debug hallucinate @a");
        context.waitTicks(5);
        Vec3 figure = context.computeOnClient(client -> {
            ShadowFigureEntity active = ClientHallucinations.active();
            check(active != null, "no shadow figure on the client");
            check(!active.isPickable() && active.isInvisible(), "figure is pickable or would show a hitbox");
            double half = ClientHallucinations.halfHorizontalFov(client);
            HallucinationPlacement.Band band = HallucinationPlacement.band(half);
            double distance = Math.hypot(active.getX() - client.player.getX(), active.getZ() - client.player.getZ());
            double angle = Math.abs(HallucinationPlacement.relativeAngle(client.player.getYRot(), active.getX() - client.player.getX(), active.getZ() - client.player.getZ()));
            check(distance >= 7.5 && distance <= 13.5, "figure " + distance + " blocks away");
            check(angle >= band.min() - 0.01 && angle <= band.max() + 0.01,
                    String.format(Locale.ROOT, "figure %.1f degrees off the view, band %.1f-%.1f (half-FOV %.1f)", angle, band.min(), band.max(), half));
            return active.position();
        });
        UUID figureId = context.computeOnClient(client -> ClientHallucinations.active().getUUID());
        boolean onServer = server.computeOnServer(s -> s.overworld().getEntity(figureId) != null);
        check(!onServer, "the figure exists on the server");
        // It appears just inside the screen edge, so the screenshot is taken straight away, without turning.
        context.waitTicks(2);
        boolean stillThere = context.computeOnClient(client -> ClientHallucinations.active() != null);
        check(stillThere, "figure vanished without the player turning");
        context.takeScreenshot("06-shadow-figure-on-appearance");
        // Let it stand there (unlooked-at, it lasts 10 s) long enough for several presence sounds.
        context.waitTicks(90);
        context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, figure.add(0, 1.6, 0)));
        context.waitTicks(3);
        boolean gone = context.computeOnClient(client -> ClientHallucinations.active() == null);
        check(gone, "figure didn't vanish when looked at");
        context.takeScreenshot("07-shadow-figure-gone-after-looking");
        context.waitTicks(40);
        server.runCommand("execute as @p run paranormalcraft debug trace stop");
        context.waitTicks(5);
        checkFigureSounds(readNewestTrace(context));
    }

    /**
     * Opens the creative inventory on our tab: it holds the goggles, EMF Reader, thermometer and spawn egg.
     */
    private static void creativeTab(ClientGameTestContext context, TestServerContext server) {

        server.runCommand("gamemode creative @a");
        context.waitTicks(5);
        context.runOnClient(client -> {
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(),
                    client.options.operatorItemsTab().get());
            client.gui.setScreen(screen);
            check(screen.setSelectedTab(ModCreativeTabs.PARANORMAL_CRAFT.get()), "couldn't select the Paranormal Craft tab");
        });
        context.waitTicks(5);
        List<String> shown = context.computeOnClient(client -> {
            check(client.gui.screen() instanceof CreativeModeInventoryScreen, "creative inventory isn't open");
            CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) client.gui.screen();
            return screen.getMenu().items.stream().filter(stack -> !stack.isEmpty())
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).toList();
        });
        for (var item : List.of(ModItems.SPECTRAL_GOGGLES, ModItems.EMF_READER, ModItems.SPIRIT_THERMOMETER, ModItems.LINGERING_SPIRIT_SPAWN_EGG)) {
            String id = BuiltInRegistries.ITEM.getKey(item.get()).toString();
            check(shown.contains(id), "creative tab is missing " + id + " (shows " + shown + ")");
        }
        context.takeScreenshot("08-creative-tab");
        context.runOnClient(client -> client.gui.setScreen(null));
    }

    /**
     * A ghost in front of a player without goggles manifests; once the manifest ends its opacity must step down to 0
     * over about 10 ticks rather than vanish, while staying unpickable.
     */
    private static void manifestFadeOut(ClientGameTestContext context, TestServerContext server) {

        server.runCommand("kill @e[type=" + GHOST + "]");
        server.runCommand("item replace entity @a armor.head with air");
        server.runCommand("item replace entity @a weapon.mainhand with air");
        server.runCommand("tp @a 0.5 -60 0.5 180 0");
        server.runCommand("summon " + GHOST + " 0.5 -59.5 -3.5 {NoAI:1b}");
        context.waitTicks(10);
        server.runCommand("execute as @p run paranormalcraft debug trace start");
        context.waitTicks(3);
        server.runCommand("paranormalcraft debug manifest @e[type=" + GHOST + "]");
        // The manifest lasts 30 ticks; the fade-out takes the next 10. Screenshot about halfway through it.
        context.waitTicks(GhostEntity.MANIFEST_TICKS + 5);
        server.runCommand("execute as @p run paranormalcraft debug screenshot fade_out_middle");
        context.waitTicks(25);
        server.runCommand("execute as @p run paranormalcraft debug trace stop");
        context.waitTicks(5);
        List<JsonObject> trace = readNewestTrace(context);
        List<JsonObject> renders = trace.stream().filter(line -> line.get("type").getAsString().equals("ghost_render")).toList();
        check(renders.stream().anyMatch(line -> line.get("manifesting").getAsBoolean() && line.get("alpha").getAsDouble() > 0.59),
                "the manifest never reached full opacity: " + renders);
        int end = -1;
        for (int i = 1; i < renders.size(); i++) {
            if (renders.get(i - 1).get("manifesting").getAsBoolean() && !renders.get(i).get("manifesting").getAsBoolean()) {
                end = i;
                break;
            }
        }
        check(end > 0, "no manifest end in the trace: " + renders);
        List<Double> alphas = new ArrayList<>();
        for (int i = end; i < renders.size(); i++) {
            JsonObject line = renders.get(i);
            check(!line.get("pickable").getAsBoolean(), "ghost pickable during the fade-out: " + line);
            alphas.add(line.get("alpha").getAsDouble());
            if (line.get("alpha").getAsDouble() == 0.0) {
                check(!line.get("visible").getAsBoolean(), "ghost still drawn at alpha 0: " + line);
                break;
            }
        }
        System.out.println("[client gametest] fade-out alphas: " + alphas);
        check(alphas.size() >= 9 && alphas.size() <= 12, "fade-out took " + alphas.size() + " frames: " + alphas);
        for (int i = 1; i < alphas.size(); i++) {
            check(alphas.get(i) < alphas.get(i - 1), "fade-out not falling step by step: " + alphas);
        }
        check(alphas.getFirst() < 0.6 && alphas.getFirst() > 0.45 && alphas.getLast() == 0.0, "fade-out alphas " + alphas);
        boolean shot = trace.stream().anyMatch(line -> line.get("type").getAsString().equals("screenshot")
                && line.get("path").getAsString().endsWith("fade_out_middle.png") && Files.exists(Path.of(line.get("path").getAsString())));
        check(shot, "the debug screenshot wasn't saved");
        server.runCommand("kill @e[type=" + GHOST + "]");
    }

    /**
     * Presence sounds: the first as the figure appears, then every 20–30 ticks, all at the figure (head height), all
     * local, and none after it vanished.
     */
    private static void checkFigureSounds(List<JsonObject> trace) {

        JsonObject spawn = trace.stream().filter(line -> line.get("type").getAsString().equals("figure_spawn")).findFirst().orElse(null);
        JsonObject vanish = trace.stream().filter(line -> line.get("type").getAsString().equals("figure_vanish")).findFirst().orElse(null);
        check(spawn != null && vanish != null, "figure spawn/vanish missing from the trace");
        check(vanish.get("reason").getAsString().equals("looked_at"), "figure vanished because " + vanish.get("reason"));
        List<JsonObject> sounds = trace.stream().filter(line -> line.get("type").getAsString().equals("figure_sound")).toList();
        check(sounds.size() >= 3, "only " + sounds.size() + " presence sound(s)");
        long spawnTime = spawn.get("t").getAsLong();
        long vanishTime = vanish.get("t").getAsLong();
        check(sounds.getFirst().get("t").getAsLong() - spawnTime <= 2, "first presence sound came late: " + sounds.getFirst());
        for (int i = 0; i < sounds.size(); i++) {
            JsonObject sound = sounds.get(i);
            check(Math.abs(sound.get("x").getAsDouble() - spawn.get("x").getAsDouble()) < 0.01
                    && Math.abs(sound.get("y").getAsDouble() - (spawn.get("y").getAsDouble() + 1.6)) < 0.01
                    && Math.abs(sound.get("z").getAsDouble() - spawn.get("z").getAsDouble()) < 0.01, "sound not at the figure: " + sound + " vs " + spawn);
            check(sound.get("t").getAsLong() <= vanishTime, "presence sound after the figure vanished: " + sound);
            if (i > 0) {
                long gap = sound.get("t").getAsLong() - sounds.get(i - 1).get("t").getAsLong();
                check(gap >= 20 && gap <= 30, "presence sounds " + gap + " ticks apart");
            }
        }
        // Where an audio device exists, the played sounds are traced too, and must be the same local, positioned ones.
        List<JsonObject> played = trace.stream().filter(line -> line.get("type").getAsString().equals("sound")
                && line.get("id").getAsString().equals("paranormalcraft:entity.shadow_figure.presence")).toList();
        for (JsonObject sound : played) {
            check(sound.get("local").getAsBoolean() && !sound.get("relative").getAsBoolean(), "presence sound not local and positioned: " + sound);
        }
        System.out.println("[client gametest] presence sounds: " + sounds.size() + " traced, " + played.size() + " played by the sound engine; vanished at "
                + (vanishTime - spawnTime) + " ticks");
    }

    /**
     * The client's newest trace file, one JSON object per line.
     */
    private static List<JsonObject> readNewestTrace(ClientGameTestContext context) {

        Path dir = context.computeOnClient(client -> client.gameDirectory.toPath().resolve("paranormalcraft-trace"));
        try (Stream<Path> files = Files.list(dir)) {
            // Client traces only (trace-*); the integrated server writes server-trace-* to the same folder.
            Path newest = files.filter(path -> path.getFileName().toString().startsWith("trace-") && path.getFileName().toString().endsWith(".jsonl"))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified())).orElseThrow();
            return Files.readAllLines(newest).stream().map(line -> JsonParser.parseString(line).getAsJsonObject()).toList();
        } catch (IOException e) {
            throw new AssertionError("could not read the client trace in " + dir, e);
        }
    }

    private static void check(boolean condition, String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
