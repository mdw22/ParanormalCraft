package io.github.mdw22.paranormalcraft.client.debug;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.network.DebugClientPayload;
import io.github.mdw22.paranormalcraft.network.DebugReplyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client side of the debug toolkit: carries out what a {@link DebugClientPayload} asks and replies to the server with
 * the result (shown in the player's chat and the server log).
 */
public final class ClientDebugTools {

    public static final String SCREENSHOT_DIR = "paranormalcraft-shots";

    private ClientDebugTools() {
    }

    public static void handle(DebugClientPayload payload) {

        Minecraft minecraft = Minecraft.getInstance();
        try {
            switch (payload.action()) {
                case "screenshot" -> screenshot(minecraft, payload.arg(0, "shot"), Integer.parseInt(payload.arg(1, "854")), Integer.parseInt(payload.arg(2, "480")));
                case "trace_start" -> reply("Client trace started: " + ClientTrace.start());
                case "trace_stop" -> {
                    Path path = ClientTrace.stop();
                    reply(path == null ? "No client trace was running" : "Client trace written: " + path);
                }
                case "trace_mark" -> ClientTrace.event("mark", line -> line.addProperty("label", payload.arg(0, "")));
                case "trace_snapshot" -> {
                    if (!ClientTrace.active()) {
                        reply("No client trace is running; start one first");
                    } else {
                        ClientTrace.snapshot(minecraft);
                    }
                }
                case "config_set" -> reply("Client config " + payload.arg(0, "") + " = " + ModConfigs.setClient(payload.arg(0, ""), payload.arg(1, "")) + " (in memory)");
                case "trace_mirror" -> ClientTrace.setMirroring(Boolean.parseBoolean(payload.arg(0, "false")));
                case "config_reload" -> {
                    ModConfigs.loadClient();
                    reply("Client config reloaded from disk");
                }
                default -> Constants.LOG.warn("Unknown debug client action {}", payload.action());
            }
        } catch (IOException | RuntimeException e) {
            Constants.LOG.error("Debug client action {} failed", payload.action(), e);
            reply("Client could not do " + payload.action() + ": " + e.getMessage());
        }
    }

    /**
     * Grabs the framebuffer with vanilla's screenshot code, scales it to {@code width}×{@code height} and saves it to
     * {@code <game dir>/paranormalcraft-shots/<name>.png}.
     */
    private static void screenshot(Minecraft minecraft, String name, int width, int height) {

        Path dir = minecraft.gameDirectory.toPath().resolve(SCREENSHOT_DIR);
        Path file = dir.resolve(name.endsWith(".png") ? name : name + ".png").toAbsolutePath().normalize();
        Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), image -> {
            try (image; NativeImage scaled = new NativeImage(width, height, false)) {
                image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), scaled);
                Files.createDirectories(dir);
                scaled.writeToFile(file);
                ClientTrace.event("screenshot", line -> line.addProperty("path", file.toString()));
                minecraft.execute(() -> reply("Screenshot saved: " + file));
            } catch (IOException | RuntimeException e) {
                Constants.LOG.error("Could not save screenshot {}", file, e);
                minecraft.execute(() -> reply("Screenshot failed: " + e.getMessage()));
            }
        });
    }

    /**
     * Logs the message, shows it in this client's chat and sends it to the server.
     */
    public static void reply(String message) {

        Constants.LOG.info("[debug] {}", message);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(message));
        }
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new DebugReplyPayload(message)));
        }
    }
}
