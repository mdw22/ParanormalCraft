package io.github.mdw22.paranormalcraft.fabric.gametest;

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
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Client GameTests for the things that have to be seen. One superflat world, four checks, screenshots saved to
 * {@code fabric/build/run/clientGameTest/screenshots}:
 * <ol>
 *     <li>a ghost is hidden without goggles and shown with them ({@code canSee} asserted on the client);</li>
 *     <li>the EMF Reader model at level 1 and at level 5;</li>
 *     <li>the thermometer's action bar while standing on a cold spot;</li>
 *     <li>a shadow figure from {@code debug hallucinate} that vanishes once looked at.</li>
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
            shadowFigure(context, server);
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
        server.runCommand("tp @a 0.5 -60 0.5 180 0");
        context.waitTicks(10);
        server.runCommand("paranormalcraft debug hallucinate @a");
        context.waitTicks(5);
        Vec3 figure = context.computeOnClient(client -> {
            ShadowFigureEntity active = ClientHallucinations.active();
            check(active != null, "no shadow figure on the client");
            check(!active.isPickable() && active.isInvisible(), "figure is pickable or would show a hitbox");
            double distance = Math.hypot(active.getX() - client.player.getX(), active.getZ() - client.player.getZ());
            double angle = Math.abs(HallucinationPlacement.relativeAngle(client.player.getYRot(), active.getX() - client.player.getX(), active.getZ() - client.player.getZ()));
            check(distance >= 9.5 && distance <= 16.5, "figure " + distance + " blocks away");
            check(angle >= 59.0 && angle <= 81.0, "figure " + angle + " degrees off the view");
            return active.position();
        });
        UUID figureId = context.computeOnClient(client -> ClientHallucinations.active().getUUID());
        boolean onServer = server.computeOnServer(s -> s.overworld().getEntity(figureId) != null);
        check(!onServer, "the figure exists on the server");
        // It appears 60-80° off the view, outside the default ~51° half-width of the screen, so it is only seen while
        // turning toward it. Turn to 40° off (visible, but not yet the 20° that makes it vanish) for the screenshot.
        context.runOnClient(client -> {
            double off = HallucinationPlacement.relativeAngle(client.player.getYRot(), figure.x - client.player.getX(), figure.z - client.player.getZ());
            client.player.setYRot((float) (client.player.getYRot() + off - Math.copySign(40.0, off)));
            client.player.setXRot(0.0F);
        });
        context.waitTicks(2);
        boolean stillThere = context.computeOnClient(client -> ClientHallucinations.active() != null);
        check(stillThere, "figure vanished at 40 degrees off the view");
        context.takeScreenshot("06-shadow-figure-in-view");
        context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, figure.add(0, 1.6, 0)));
        context.waitTicks(3);
        boolean gone = context.computeOnClient(client -> ClientHallucinations.active() == null);
        check(gone, "figure didn't vanish when looked at");
        context.takeScreenshot("07-shadow-figure-gone-after-looking");
    }

    private static void check(boolean condition, String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
