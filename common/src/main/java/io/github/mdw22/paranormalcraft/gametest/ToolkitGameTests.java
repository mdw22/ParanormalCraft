package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.config.ServerConfig;
import io.github.mdw22.paranormalcraft.debug.DebugScenes;
import io.github.mdw22.paranormalcraft.debug.ScriptRunner;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * GameTests for the debug toolkit itself: scenes, pace, in-memory config and the script runner. They change global
 * state (time of day, config, pace), so they run in their own test environment ({@code paranormalcraft:toolkit}).
 */
public final class ToolkitGameTests {

    private ToolkitGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("toolkit_scene_builds_and_repeats", ToolkitGameTests::sceneBuildsAndRepeats);
        RegistrationHelper.testFunction("toolkit_pace_scales_and_resets", ToolkitGameTests::paceScalesAndResets);
        RegistrationHelper.testFunction("toolkit_config_set_in_memory", ToolkitGameTests::configSetInMemory);
        RegistrationHelper.testFunction("toolkit_script_runner", ToolkitGameTests::scriptRunner);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    private static List<GhostEntity> sceneGhosts(ServerLevel level) {

        BlockPos centre = DebugScenes.CENTRE;
        return level.getEntitiesOfClass(GhostEntity.class, new AABB(centre).inflate(DebugScenes.HALF_SIZE + 2, 40, DebugScenes.HALF_SIZE + 2));
    }

    /**
     * {@code cabin_oak_night} places the cabin and one ghost homed at the (removed) marker; {@code arena} twice gives
     * exactly one ghost homed in the middle, the platform, its edge lights and nothing left over.
     */
    private static void sceneBuildsAndRepeats(GameTestHelper helper) {

        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();
        BlockPos centre = DebugScenes.CENTRE;

        check(helper, DebugScenes.build(server, "cabin_oak_night", null).isPresent(), "cabin scene didn't build");
        List<GhostEntity> cabinGhosts = sceneGhosts(level);
        check(helper, cabinGhosts.size() == 1, "%d ghosts in the cabin scene", cabinGhosts.size());
        BlockPos home = cabinGhosts.getFirst().getHome();
        check(helper, home != null && level.getBlockState(home).isAir(), "cabin ghost home %s isn't the removed marker", home);
        check(helper, !level.getBlockState(home).is(ModBlocks.GHOST_MARKER.get()), "marker still there");
        check(helper, home.south().equals(cabinGhosts.getFirst().getColdSpot()),
                "cold spot %s isn't next to the marker", cabinGhosts.getFirst().getColdSpot());
        check(helper, level.getBlockState(home.below()).is(Blocks.SPRUCE_PLANKS) || level.getBlockState(home.below()).is(Blocks.OAK_PLANKS),
                "no cabin floor under the marker (%s)", level.getBlockState(home.below()));

        for (int run = 0; run < 2; run++) {
            check(helper, DebugScenes.build(server, "arena", null).isPresent(), "arena didn't build");
        }
        List<GhostEntity> ghosts = sceneGhosts(level);
        check(helper, ghosts.size() == 1, "%d ghosts after building the arena twice", ghosts.size());
        check(helper, centre.above().equals(ghosts.getFirst().getHome()) && centre.above().equals(ghosts.getFirst().getColdSpot()),
                "arena ghost home %s / cold spot %s", ghosts.getFirst().getHome(), ghosts.getFirst().getColdSpot());
        check(helper, level.getBlockState(centre).is(Blocks.GRASS_BLOCK) && level.getBlockState(centre.below()).is(Blocks.STONE),
                "platform isn't grass on stone");
        check(helper, level.getBlockState(centre.offset(-DebugScenes.HALF_SIZE, 1, 0)).is(Blocks.LIGHT), "no light block on the edge");
        check(helper, level.getBlockState(centre.offset(0, 1, -6)).isAir(), "cabin left over in the arena");
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(centre).inflate(30));
        check(helper, items.isEmpty(), "%d item(s) left over", items.size());
        ghosts.forEach(GhostEntity::discard);
        helper.succeed();
    }

    /**
     * Fast pace makes a new ghost's manifest timer at most a tenth of the configured maximum; the reset that runs when
     * the server stops ({@code CommonEvents.onServerStopped}) puts it back.
     */
    private static void paceScalesAndResets(GameTestHelper helper) {

        ServerConfig config = ModConfigs.server();
        int maxTicks = config.manifestIntervalMaxSeconds() * 20;
        ModConfigs.setPace(ModConfigs.FAST_PACE);
        check(helper, ModConfigs.paced(maxTicks) == Math.round(maxTicks * ModConfigs.FAST_PACE), "paced(%d) = %d", maxTicks, ModConfigs.paced(maxTicks));
        LingeringSpiritEntity ghost = helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), new BlockPos(20, 2, 20));
        helper.runAfterDelay(3, () -> {
            int cooldown = ghost.manifestCooldown();
            check(helper, cooldown > 0 && cooldown <= maxTicks * ModConfigs.FAST_PACE + 1, "manifest timer %d at fast pace (max %d)", cooldown, maxTicks);
            ghost.discard();
            ModConfigs.resetSession();
            check(helper, ModConfigs.pace() == 1.0 && ModConfigs.paced(maxTicks) == maxTicks, "pace %s not reset", ModConfigs.pace());
            helper.succeed();
        });
    }

    /**
     * {@code config set} changes the value (clamped like the file would be) without touching the file on disk.
     */
    private static void configSetInMemory(GameTestHelper helper) {

        ServerConfig original = ModConfigs.server();
        Path file = Services.PLATFORM.getConfigDir().resolve(ServerConfig.FILE_NAME);
        byte[] before = read(file);
        try {
            String applied = ModConfigs.setServer("manifestIntervalSeconds.min", "7");
            check(helper, ModConfigs.server().manifestIntervalMinSeconds() == 7 && applied.equals("7"), "min is %d (%s)",
                    ModConfigs.server().manifestIntervalMinSeconds(), applied);
            ModConfigs.setServer("ghostSpawnMultiplier", "9");
            check(helper, ModConfigs.server().ghostSpawnMultiplier() == 4.0, "multiplier not clamped: %s", ModConfigs.server().ghostSpawnMultiplier());
            ModConfigs.setServer("hauntingEnabled", "false");
            check(helper, !ModConfigs.server().hauntingEnabled(), "hauntingEnabled still on");
            check(helper, "false".equals(ModConfigs.get("hauntingEnabled")), "get returned %s", ModConfigs.get("hauntingEnabled"));
            check(helper, Arrays.equals(before, read(file)), "the config file changed on disk");
        } finally {
            ModConfigs.setServerForTesting(original);
        }
        helper.succeed();
    }

    private static byte[] read(Path file) {

        try {
            return Files.exists(file) ? Files.readAllBytes(file) : new byte[0];
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A script's waits are counted in game ticks, it runs commands, and a bad line stops it with that line's number.
     */
    private static void scriptRunner(GameTestHelper helper) {

        MinecraftServer server = helper.getLevel().getServer();
        List<ScriptRunner.Result> results = new ArrayList<>();
        var source = server.createCommandSourceStack().withSuppressedOutput();
        ScriptRunner.start(source, "good", List.of("# a comment", "", "wait 10", "time query gametime",
                "execute positioned 0 0 0 run time query gametime", "wait 5"), results::add);
        ScriptRunner.start(source, "bad", List.of("wait 3", "say hello", "this_is_not_a_command at all", "say never"), results::add);
        BlockPos ghostAt = helper.absolutePos(new BlockPos(20, 2, 20));
        String selector = String.format(Locale.ROOT, "@e[type=paranormalcraft:lingering_spirit,x=%d,y=%d,z=%d,distance=..2,limit=1]",
                ghostAt.getX(), ghostAt.getY(), ghostAt.getZ());
        helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), new BlockPos(20, 2, 20));
        var atGhost = source.withPosition(net.minecraft.world.phys.Vec3.atCenterOf(ghostAt));
        ScriptRunner.start(atGhost, "expect_ok", List.of(
                "wait 2",
                "paranormalcraft debug manifest " + selector,
                "expect trace manifest_start ticks=30 within 5",
                "expect dump ghosts.length>=1 ghosts[*].manifesting=true"), results::add);
        ScriptRunner.start(atGhost, "expect_fail", List.of(
                "wait 2",
                "paranormalcraft debug presence " + selector + " 15",
                "expect trace presence to=3 within 4"), results::add);
        helper.succeedWhen(() -> {
            check(helper, results.size() == 4, "%d script(s) finished", results.size());
            ScriptRunner.Result expectOk = results.stream().filter(result -> result.name().equals("expect_ok")).findFirst().orElseThrow();
            ScriptRunner.Result expectFail = results.stream().filter(result -> result.name().equals("expect_fail")).findFirst().orElseThrow();
            check(helper, expectOk.ok(), "expect_ok: %s", expectOk);
            check(helper, !expectFail.ok() && expectFail.line() == 3 && expectFail.message().contains("closest")
                    && expectFail.message().contains("\"to\":15.0"), "expect_fail: %s", expectFail);
            ScriptRunner.Result good = results.stream().filter(result -> result.name().equals("good")).findFirst().orElseThrow();
            ScriptRunner.Result bad = results.stream().filter(result -> result.name().equals("bad")).findFirst().orElseThrow();
            check(helper, good.ok() && good.gameTicks() == 15, "good script: %s", good);
            check(helper, !bad.ok() && bad.line() == 3 && bad.gameTicks() == 3, "bad script: %s", bad);
        });
    }
}
