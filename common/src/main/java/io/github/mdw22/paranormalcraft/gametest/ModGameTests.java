package io.github.mdw22.paranormalcraft.gametest;

import io.github.mdw22.paranormalcraft.capture.PresenceRules;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.ColdSpots;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Presence;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import io.github.mdw22.paranormalcraft.tool.EmfReadings;
import io.github.mdw22.paranormalcraft.tool.ThermometerReadings;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Server GameTests, run by {@code ./gradlew :neoforge:runGameTestServer} and {@code :fabric:runGameTest}. Each
 * function has a {@code test_instance} JSON under {@code data/paranormalcraft/test_instance}, using the
 * {@code paranormalcraft:arena} structure: 40×10×40 with a stone floor at y=0. Tests sensitive to other ghosts nearby
 * (EMF, thermometer) use their own test environment so they run in a batch of their own.
 */
public final class ModGameTests {

    /** Ghosts float here, in the middle of the arena. */
    private static final BlockPos CENTRE = new BlockPos(20, 2, 20);

    private ModGameTests() {
    }

    public static void init() {

        RegistrationHelper.testFunction("ghost_hit_needs_visibility", ModGameTests::ghostHitNeedsVisibility);
        RegistrationHelper.testFunction("environmental_damage_rejected", ModGameTests::environmentalDamageRejected);
        RegistrationHelper.testFunction("presence_zero_disperses", ModGameTests::presenceZeroDisperses);
        RegistrationHelper.testFunction("ghost_returns_home", ModGameTests::ghostReturnsHome);
        RegistrationHelper.testFunction("emf_level_by_distance", ModGameTests::emfLevelByDistance);
        RegistrationHelper.testFunction("thermometer_cold_spot", ModGameTests::thermometerColdSpot);
        RegistrationHelper.testFunction("spectral_arrow_glows_without_damage", ModGameTests::spectralArrowGlowsWithoutDamage);
        RegistrationHelper.testFunction("cold_spot_is_roofed", ModGameTests::coldSpotIsRoofed);
        RegistrationHelper.testFunction("ghost_marker_spawns_homed_ghost", ModGameTests::ghostMarkerSpawnsHomedGhost);
        HauntingGameTests.init();
        GhostBehaviourGameTests.init();
        ItemGameTests.init();
        ToolkitGameTests.init();
        CabinGameTests.init();
        CaptureGameTests.init();
        ContainmentGameTests.init();
    }

    private static LingeringSpiritEntity frozenGhost(GameTestHelper helper) {

        return helper.spawnWithNoFreeWill(ModEntities.LINGERING_SPIRIT.get(), CENTRE);
    }

    private static void check(GameTestHelper helper, boolean condition, String message, Object... args) {

        if (!condition) {
            throw helper.assertionException(Component.literal(String.format(Locale.ROOT, message, args)));
        }
    }

    private static void checkClose(GameTestHelper helper, double expected, double actual, String what) {

        check(helper, Math.abs(expected - actual) < 1.0E-6, "%s: expected %.3f, got %.3f", what, expected, actual);
    }

    /**
     * The same punch is rejected from a player without goggles and accepted once they wear them.
     */
    private static void ghostHitNeedsVisibility(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        DamageSource punch = helper.getLevel().damageSources().playerAttack(player);

        boolean hurtWithout = ghost.hurtServer(helper.getLevel(), punch, 4.0F);
        check(helper, !hurtWithout && ghost.getPresence() == Presence.MAX, "hit without goggles counted (hurt=%s, presence=%.1f)", hurtWithout, ghost.getPresence());

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
        boolean hurtWith = ghost.hurtServer(helper.getLevel(), punch, 4.0F);
        // Melee drains half the damage.
        check(helper, hurtWith && ghost.getPresence() == Presence.MAX - 2.0F, "hit with goggles didn't count (hurt=%s, presence=%.1f)", hurtWith, ghost.getPresence());
        ghost.discard();
        helper.succeed();
    }

    private static void environmentalDamageRejected(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper);
        ServerLevel level = helper.getLevel();
        DamageSources sources = level.damageSources();
        List<DamageSource> environmental = List.of(sources.fall(), sources.inFire(), sources.onFire(), sources.lava(), sources.drown(),
                sources.inWall(), sources.freeze(), sources.explosion(null, null), sources.generic(), sources.magic(), sources.cactus());
        for (DamageSource source : environmental) {
            boolean hurt = ghost.hurtServer(level, source, 5.0F);
            check(helper, !hurt, "%s damage counted", source.getMsgId());
        }
        check(helper, ghost.getPresence() == Presence.MAX && !ghost.isRemoved(), "Presence changed to %.1f", ghost.getPresence());
        ghost.discard();
        helper.succeed();
    }

    /**
     * Hits never disperse: a huge valid hit leaves the ghost at the Presence floor. Only the debug command (setting
     * Presence to 0) and /kill do.
     */
    private static void presenceZeroDisperses(GameTestHelper helper) {

        LingeringSpiritEntity byHits = frozenGhost(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
        byHits.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), Presence.MAX * 4.0F);
        check(helper, !byHits.isRemoved() && byHits.getPresence() == PresenceRules.FLOOR, "a huge hit: removed=%s, %.1f left",
                byHits.isRemoved(), byHits.getPresence());
        byHits.discard();

        LingeringSpiritEntity bySetting = frozenGhost(helper);
        bySetting.setPresence(helper.getLevel(), 0.0F);
        check(helper, bySetting.isRemoved(), "ghost survived Presence being set to 0");

        LingeringSpiritEntity byKill = frozenGhost(helper);
        byKill.kill(helper.getLevel());
        check(helper, byKill.isRemoved(), "ghost survived /kill");
        helper.succeed();
    }

    /**
     * Pushed 22 blocks from home, the ghost drifts back inside its 16-block leash.
     */
    private static void ghostReturnsHome(GameTestHelper helper) {

        LingeringSpiritEntity ghost = helper.spawn(ModEntities.LINGERING_SPIRIT.get(), CENTRE);
        helper.runAfterDelay(2, () -> {
            check(helper, ghost.getHome() != null, "home was not set on the first tick");
            Vec3 far = helper.absoluteVec(new Vec3(CENTRE.getX() + 0.5, CENTRE.getY(), 38.5));
            ghost.teleportTo(far.x, far.y, far.z);
            check(helper, ghost.distanceToHomeSqr() > GhostEntity.LEASH_RADIUS * GhostEntity.LEASH_RADIUS, "teleport didn't leave the leash");
            helper.succeedWhen(() -> {
                check(helper, ghost.distanceToHomeSqr() <= GhostEntity.LEASH_RADIUS * GhostEntity.LEASH_RADIUS,
                        "still %.1f blocks from home", Math.sqrt(ghost.distanceToHomeSqr()));
                ghost.discard();
            });
        });
    }

    private static void emfLevelByDistance(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper);
        ServerLevel level = helper.getLevel();
        Vec3 at = ghost.position();
        double[][] expectations = {{0, 5}, {3, 5}, {7, 4}, {12, 3}, {20, 2}, {30, 1}};
        for (double[] expectation : expectations) {
            int levelThere = EmfReadings.levelAt(level, at.add(expectation[0], 0, 0));
            check(helper, levelThere == (int) expectation[1], "%.0f blocks: expected EMF %d, got %d", expectation[0], (int) expectation[1], levelThere);
        }
        // Walls don't matter: put stone between the ghost and the reader.
        for (int y = 1; y < 6; y++) {
            for (int z = 17; z < 24; z++) {
                helper.setBlock(new BlockPos(CENTRE.getX() + 2, y, z), Blocks.STONE);
            }
        }
        int throughWall = EmfReadings.levelAt(level, at.add(3, 0, 0));
        check(helper, throughWall == 5, "a wall changed the reading to %d", throughWall);
        ghost.discard();
        helper.succeed();
    }

    /**
     * No roof in the arena, so the cold spot falls back to home. Readings: −10° at the centre, −5° at 2 blocks, 0 at 5.
     */
    private static void thermometerColdSpot(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper);
        helper.runAfterDelay(2, () -> {
            BlockPos coldSpot = ghost.getColdSpot();
            check(helper, coldSpot != null, "no cold spot");
            ServerLevel level = helper.getLevel();
            Vec3 centre = Vec3.atBottomCenterOf(coldSpot);
            checkClose(helper, -10.0, ThermometerReadings.coldSpotOffset(level, centre), "offset at the centre");
            checkClose(helper, -5.0, ThermometerReadings.coldSpotOffset(level, centre.add(2, 0, 0)), "offset at 2 blocks");
            checkClose(helper, 0.0, ThermometerReadings.coldSpotOffset(level, centre.add(5, 0, 0)), "offset at 5 blocks");
            double atCentre = ThermometerReadings.readWithoutNoise(level, centre);
            double outside = ThermometerReadings.readWithoutNoise(level, centre.add(5, 0, 0));
            checkClose(helper, -10.0, atCentre - outside, "full reading difference centre vs 5 blocks");
            ghost.discard();
            helper.succeed();
        });
    }

    private static void spectralArrowGlowsWithoutDamage(GameTestHelper helper) {

        LingeringSpiritEntity ghost = frozenGhost(helper);
        // TODO(26.3): makeMockServerPlayerInLevel is deprecated; switch once a replacement exists.
        ServerPlayer archer = helper.makeMockServerPlayerInLevel();
        archer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        Vec3 start = helper.absoluteVec(new Vec3(CENTRE.getX() + 0.5, CENTRE.getY() + 0.6, CENTRE.getZ() - 4.5));
        SpectralArrow arrow = new SpectralArrow(helper.getLevel(), archer, new ItemStack(Items.SPECTRAL_ARROW), null);
        arrow.setPos(start);
        arrow.shoot(0, 0, 1, 2.0F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);
        helper.succeedWhen(() -> {
            check(helper, ghost.hasEffect(MobEffects.GLOWING), "ghost isn't glowing yet");
            check(helper, ghost.getPresence() == Presence.MAX, "the arrow drained Presence to %.1f", ghost.getPresence());
            ghost.discard();
            archer.level().getServer().getPlayerList().remove(archer);
        });
    }

    /**
     * The cabin's marker block turns into a ghost homed at the marker (multiplier 1.0: the first ghost always spawns)
     * and removes itself.
     */
    private static void ghostMarkerSpawnsHomedGhost(GameTestHelper helper) {

        BlockPos marker = new BlockPos(20, 1, 20);
        helper.setBlock(marker, ModBlocks.GHOST_MARKER.get());
        helper.runAfterDelay(3, () -> {
            helper.assertBlockNotPresent(ModBlocks.GHOST_MARKER.get(), marker);
            BlockPos home = helper.absolutePos(marker);
            List<LingeringSpiritEntity> ghosts = helper.getLevel().getEntitiesOfClass(LingeringSpiritEntity.class, new AABB(home).inflate(2));
            check(helper, !ghosts.isEmpty() && ghosts.size() <= 2, "%s ghost(s) spawned", ghosts.size());
            for (LingeringSpiritEntity ghost : ghosts) {
                check(helper, home.equals(ghost.getHome()), "ghost home %s, expected %s", ghost.getHome(), home);
                ghost.discard();
            }
            helper.succeed();
        });
    }

    /**
     * With a small roofed shelter inside the home radius, the cold spot lands under it.
     */
    private static void coldSpotIsRoofed(GameTestHelper helper) {

        for (int x = 25; x <= 27; x++) {
            for (int z = 25; z <= 27; z++) {
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
            }
        }
        LingeringSpiritEntity ghost = frozenGhost(helper);
        helper.runAfterDelay(2, () -> {
            BlockPos coldSpot = ghost.getColdSpot();
            check(helper, coldSpot != null && !coldSpot.equals(ghost.getHome()), "cold spot fell back to home (%s)", coldSpot);
            check(helper, ColdSpots.isRoofed(helper.getLevel(), coldSpot), "cold spot %s isn't roofed", coldSpot);
            boolean underShelter = false;
            for (int x = 25; x <= 27; x++) {
                for (int z = 25; z <= 27; z++) {
                    underShelter |= coldSpot.equals(helper.absolutePos(new BlockPos(x, 1, z)));
                }
            }
            check(helper, underShelter, "cold spot %s isn't under the shelter", coldSpot);
            ghost.discard();
            helper.succeed();
        });
    }
}
