package io.github.mdw22.paranormalcraft.debug;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.haunting.HauntingScheduler;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /paranormalcraft debug scene <name>}: fixed, repeatable test setups, all defined here.
 * <p>
 * Every scene is built on a floating 48×48 grass platform (stone underneath, light blocks along the edges) centred
 * on {@link #CENTRE}, far from spawn and clear of terrain and water. Building a scene first clears whatever the last
 * one left (blocks, ghosts, items, paintings), sets the time and weather, places a cabin if it uses one, spawns its
 * ghosts with a fixed home and cold spot, and puts the player (and Dev2, if online) at a fixed spot and view in
 * survival with the {@link DebugKit}. Running a scene twice gives the same result.
 * <p>
 * To add one: write a {@code Scene} lambda and add it to {@link #SCENES}.
 */
public final class DebugScenes {

    /** The platform's centre block (the grass surface is at this y). */
    public static final BlockPos CENTRE = new BlockPos(100_000, 200, 100_000);
    public static final int HALF_SIZE = 24;
    /** Cleared above the platform. */
    private static final int CLEAR_HEIGHT = 32;
    private static final int STONE_DEPTH = 3;
    private static final long NOON = 6000;
    private static final long MIDNIGHT = 18000;
    /** Every entity a scene creates carries this tag. */
    public static final String SCENE_TAG = "paranormalcraft_scene";
    /** Ticks after a build when leftovers from earlier builds are swept away. */
    private static final long[] SWEEP_DELAYS = {5, 20, 40};
    /** The second player a scene places, if online (the {@code client2} dev run). */
    public static final String SECOND_PLAYER = "Dev2";
    /** Cabin templates: where the marker and cold spot sit, relative to the template's corner (rotation none). */
    private static final BlockPos CABIN_MARKER = new BlockPos(9, 1, 5);
    private static final BlockPos CABIN_COLD_SPOT = new BlockPos(9, 1, 6);
    /** The cabin template's corner, placed so the house is centred on the platform. */
    private static final BlockPos CABIN_ORIGIN = CENTRE.offset(-7, 0, -6);

    /** Builds one scene; returns where the players stand. */
    @FunctionalInterface
    private interface Scene {

        Placement build(ServerLevel level);
    }

    /**
     * Where the players go: first player position, yaw and pitch; the second player stands {@code secondOffset}
     * away with the same view. {@code ghosts} lists what was spawned.
     */
    public record Placement(Vec3 position, float yaw, float pitch, Vec3 secondOffset, List<LingeringSpiritEntity> ghosts) {
    }

    private static final Map<String, Scene> SCENES = new LinkedHashMap<>();

    static {
        SCENES.put("arena", level -> arena(level, NOON));
        SCENES.put("arena_night", level -> arena(level, MIDNIGHT));
        SCENES.put("cabin_oak_night", level -> cabin(level, "oak", true));
        SCENES.put("cabin_spruce_night", level -> cabin(level, "spruce", true));
        SCENES.put("haunt_props", DebugScenes::hauntProps);
        SCENES.put("two_player", DebugScenes::twoPlayer);
        SCENES.put("figure_indoor", level -> cabin(level, "oak", false));
    }

    private DebugScenes() {
    }

    public static Set<String> names() {

        return SCENES.keySet();
    }

    /**
     * Builds {@code name} in the overworld and places {@code player} (if not null) and Dev2 (if online).
     *
     * @return what was built, or empty for an unknown name
     */
    public static Optional<Placement> build(MinecraftServer server, String name, @Nullable ServerPlayer player) {

        Scene scene = SCENES.get(name);
        if (scene == null) {
            return Optional.empty();
        }
        ServerLevel level = server.overworld();
        prepare(level);
        Placement placement = scene.build(level);
        // Tag what this build made; entities from an earlier build (loaded from disk a little after their chunks, so
        // missed by prepare) carry the tag too, and the sweeps below remove them.
        Set<UUID> current = new HashSet<>();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area(), entity -> !(entity instanceof Player))) {
            entity.addTag(SCENE_TAG);
            current.add(entity.getUUID());
        }
        for (long delay : SWEEP_DELAYS) {
            HauntingScheduler.schedule(level, delay, () -> sweep(level, current));
        }
        List<ServerPlayer> players = new ArrayList<>();
        if (player != null) {
            players.add(player);
        }
        ServerPlayer second = server.getPlayerList().getPlayerByName(SECOND_PLAYER);
        if (second != null && second != player) {
            players.add(second);
        }
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer target = players.get(i);
            Vec3 at = i == 0 ? placement.position() : placement.position().add(placement.secondOffset());
            target.setGameMode(GameType.SURVIVAL);
            target.teleportTo(level, at.x, at.y, at.z, Set.of(), placement.yaw(), placement.pitch(), true);
            target.setDeltaMovement(Vec3.ZERO);
            DebugKit.give(target, true);
        }
        Constants.LOG.info("Built debug scene {} at {} ({} ghost(s))", name, CENTRE.toShortString(), placement.ghosts().size());
        return Optional.of(placement);
    }

    /**
     * Removes scene-tagged entities that this build didn't make (left over from an earlier one).
     */
    private static void sweep(ServerLevel level, Set<UUID> current) {

        for (Entity entity : level.getEntitiesOfClass(Entity.class, area(), entity -> entity.entityTags().contains(SCENE_TAG))) {
            if (!current.contains(entity.getUUID())) {
                Constants.LOG.info("Scene sweep: removed leftover {} at {}", entity.getType().builtInRegistryHolder().key().identifier(), entity.blockPosition().toShortString());
                entity.discard();
            }
        }
    }

    private static AABB area() {

        int min = -HALF_SIZE - 2;
        int max = HALF_SIZE + 1;
        return new AABB(CENTRE.getX() + min, CENTRE.getY() - STONE_DEPTH - 2, CENTRE.getZ() + min,
                CENTRE.getX() + max + 1, CENTRE.getY() + CLEAR_HEIGHT + 1, CENTRE.getZ() + max + 1);
    }

    // ---- the platform ----

    /**
     * Generates the chunks, clears the area (blocks and non-player entities) and lays the platform.
     */
    private static void prepare(ServerLevel level) {

        int min = -HALF_SIZE - 2;
        int max = HALF_SIZE + 1;
        for (int cx = (CENTRE.getX() + min) >> 4; cx <= (CENTRE.getX() + max) >> 4; cx++) {
            for (int cz = (CENTRE.getZ() + min) >> 4; cz <= (CENTRE.getZ() + max) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area(), entity -> !(entity instanceof Player))) {
            entity.discard();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                boolean inside = Math.abs(x + 0.5) <= HALF_SIZE && Math.abs(z + 0.5) <= HALF_SIZE;
                boolean edge = inside && (x == -HALF_SIZE || x == HALF_SIZE - 1 || z == -HALF_SIZE || z == HALF_SIZE - 1);
                for (int y = -STONE_DEPTH - 1; y <= CLEAR_HEIGHT; y++) {
                    BlockState state = Blocks.AIR.defaultBlockState();
                    if (inside && y < 0 && y >= -STONE_DEPTH) {
                        state = Blocks.STONE.defaultBlockState();
                    } else if (inside && y == 0) {
                        state = Blocks.GRASS_BLOCK.defaultBlockState();
                    } else if (edge && y == 1) {
                        state = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
                    }
                    set(level, pos.set(CENTRE.getX() + x, CENTRE.getY() + y, CENTRE.getZ() + z), state);
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {

        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static void time(ServerLevel level, long time) {

        MinecraftServer server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "time set " + (time == NOON ? "noon" : "midnight"));
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather clear");
    }

    private static LingeringSpiritEntity ghost(ServerLevel level, BlockPos home, BlockPos coldSpot) {

        LingeringSpiritEntity ghost = ModEntities.LINGERING_SPIRIT.get().create(level, EntitySpawnReason.COMMAND);
        if (ghost == null) {
            throw new IllegalStateException("Could not create a ghost");
        }
        ghost.snapTo(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, 0.0F, 0.0F);
        ghost.setHome(home, coldSpot);
        level.addFreshEntity(ghost);
        return ghost;
    }

    private static Vec3 at(int dx, int dz) {

        return Vec3.atBottomCenterOf(CENTRE.offset(dx, 1, dz));
    }

    // ---- scenes ----

    /**
     * One ghost floating in the middle; the player 8 blocks south of it, facing it.
     */
    private static Placement arena(ServerLevel level, long time) {

        time(level, time);
        BlockPos home = CENTRE.above();
        LingeringSpiritEntity ghost = ghost(level, home, home);
        return new Placement(at(0, 8), 180.0F, 0.0F, new Vec3(2, 0, 0), List.of(ghost));
    }

    /**
     * A cabin in the middle at night, its marker replaced by one ghost homed there (cold spot next to it, in the back
     * room). The player stands 14 blocks in front of the porch, facing the cabin. Without a ghost ({@code
     * figure_indoor}) the player stands inside the front room instead, facing the front wall's window.
     */
    private static Placement cabin(ServerLevel level, String wood, boolean withGhost) {

        time(level, MIDNIGHT);
        StructureTemplate template = level.getStructureManager().getOrCreate(Constants.id("abandoned_cabin_" + wood));
        template.placeInWorld(level, CABIN_ORIGIN, CABIN_ORIGIN, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        BlockPos marker = CABIN_ORIGIN.offset(CABIN_MARKER);
        if (level.getBlockState(marker).is(ModBlocks.GHOST_MARKER.get())) {
            set(level, marker, Blocks.AIR.defaultBlockState());
        } else {
            Constants.LOG.warn("Scene cabin_{}: no ghost marker at {}", wood, marker.toShortString());
        }
        // The template's paintings and item frames come back each time; the clear in prepare() removed the old ones.
        if (!withGhost) {
            // House-relative (3, 1, 3): the middle of the front room, facing south toward window A and the door.
            return new Placement(Vec3.atBottomCenterOf(CABIN_ORIGIN.offset(6, 1, 6)), 0.0F, 0.0F, new Vec3(0, 0, -1), List.of());
        }
        LingeringSpiritEntity ghost = ghost(level, marker, CABIN_ORIGIN.offset(CABIN_COLD_SPOT));
        return new Placement(at(0, 17), 180.0F, 5.0F, new Vec3(2, 0, 0), List.of(ghost));
    }

    /**
     * One ghost, held still, next to a staged painting, item frame, wall torch, lit candles, a lever, an oak door, a
     * chest and a soul torch (which it must never touch). Day, player 7 blocks south.
     */
    private static Placement hauntProps(ServerLevel level) {

        time(level, NOON);
        // A stone wall to hang things on, facing south.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                set(level, CENTRE.offset(dx, dy, -3), Blocks.STONE.defaultBlockState());
            }
        }
        set(level, CENTRE.offset(-2, 2, -2), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        set(level, CENTRE.offset(2, 1, -2), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.SOUTH));
        set(level, CENTRE.offset(1, 1, 0), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        set(level, CENTRE.offset(-4, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, CENTRE.offset(-4, 2, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(level, CENTRE.offset(3, 1, 1), Blocks.CHEST.defaultBlockState());
        set(level, CENTRE.offset(-2, 1, 1), Blocks.SOUL_TORCH.defaultBlockState());
        MinecraftServer server = level.getServer();
        BlockPos painting = CENTRE.offset(0, 2, -2);
        BlockPos frame = CENTRE.offset(2, 2, -2);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), String.format(java.util.Locale.ROOT,
                "summon painting %d %d %d {facing:0b,variant:\"minecraft:kebab\"}", painting.getX(), painting.getY(), painting.getZ()));
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), String.format(java.util.Locale.ROOT,
                "summon item_frame %d %d %d {Facing:3b,Item:{id:\"minecraft:flower_pot\",count:1}}", frame.getX(), frame.getY(), frame.getZ()));
        BlockPos home = CENTRE.above();
        LingeringSpiritEntity ghost = ghost(level, home, home);
        // Held still (no AI) so every prop stays in haunting range; haunting itself doesn't need the AI.
        ghost.setNoAi(true);
        return new Placement(at(0, 7), 180.0F, 10.0F, new Vec3(2, 0, 0), List.of(ghost));
    }

    /**
     * Night; Dev and Dev2 side by side 8 blocks south of one ghost, both facing it.
     */
    private static Placement twoPlayer(ServerLevel level) {

        time(level, MIDNIGHT);
        BlockPos home = CENTRE.above();
        LingeringSpiritEntity ghost = ghost(level, home, home);
        return new Placement(at(-1, 8), 180.0F, 0.0F, new Vec3(2, 0, 0), List.of(ghost));
    }
}
