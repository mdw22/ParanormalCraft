package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.familiar.WispWarnings;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.journal.JournalEvent;
import io.github.mdw22.paranormalcraft.journal.JournalEvents;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * A ghost that got loose following a player home (Milestone 2 part D). One per ghost.
 * <ul>
 *     <li><b>Attaching:</b> {@link #attach} (escapes from a trap or a breach always; a failed capture roll and an
 *     Agitated ghost noticing a player sometimes). The player hears one quiet "You feel watched" from behind.</li>
 *     <li><b>Following:</b> {@code GhostFollowGoal} keeps it {@link #FOLLOW_MIN}–{@link #FOLLOW_MAX} behind, unseen;
 *     it doesn't haunt or manifest; past {@link #FOLLOW_TELEPORT} it jumps to a spot out of the player's view.</li>
 *     <li><b>Waiting:</b> while its player is offline or in another dimension it stays put, and follows again once
 *     they're back within {@link #RESUME_RANGE}.</li>
 *     <li><b>Settling:</b> near the player's respawn point, or after they've stayed in one roofed spot for 2 minutes, it
 *     re-homes there (the open space above a bed or anchor; a new cold spot), slips in unseen, and haunts it under the
 *     normal rules. Recapture clears all of it.</li>
 * </ul>
 */
public final class Attachment {

    public static final double FOLLOW_MIN = Balance.Escape.FOLLOW_MIN;
    public static final double FOLLOW_MAX = Balance.Escape.FOLLOW_MAX;
    public static final double FOLLOW_TELEPORT = Balance.Escape.FOLLOW_TELEPORT;
    public static final double RESUME_RANGE = Balance.Escape.RESUME_RANGE;
    private static final int CHECK_INTERVAL = 10;

    public enum Mode {
        NONE,
        FOLLOWING,
        WAITING
    }

    private final GhostEntity ghost;
    private Mode mode = Mode.NONE;
    private @Nullable UUID player;
    private String playerName = "";
    private @Nullable BlockPos stayAnchor;
    private int stayTicks;
    /** An Agitated ghost rolls to attach once, the first time a player comes close. */
    private boolean noticed;
    /** Who it settled in on, for House Call. */
    private @Nullable UUID settledOn;
    /** Tests: fixes the Agitated attach roll. */
    private static @Nullable DoubleSupplier rollOverride;

    Attachment(GhostEntity ghost) {

        this.ghost = ghost;
    }

    public Mode mode() {

        return this.mode;
    }

    public boolean following() {

        return this.mode == Mode.FOLLOWING;
    }

    public boolean attached() {

        return this.mode != Mode.NONE;
    }

    public @Nullable UUID player() {

        return this.player;
    }

    public String playerName() {

        return this.playerName;
    }

    public @Nullable UUID settledOn() {

        return this.settledOn;
    }

    public int stayTicks() {

        return this.stayTicks;
    }

    /** From {@code CommonEvents.onServerStopped}. */
    public static void clear() {

        rollOverride = null;
    }

    /** Tests only: the roll an Agitated ghost makes when it first notices a player (null rolls for real). */
    public static void setRollOverride(@Nullable DoubleSupplier roll) {

        rollOverride = roll;
    }

    /**
     * Attaches to the nearest non-spectator player within {@code ATTACH_RANGE}, if any. Returns whether it did.
     */
    public boolean attachToNearest(ServerLevel level, String cause) {

        if (level.getNearestPlayer(this.ghost, Balance.Escape.ATTACH_RANGE) instanceof ServerPlayer nearest && !nearest.isSpectator()) {
            attach(level, nearest, cause);
            return true;
        }
        ServerTrace.ghost("attach_none", this.ghost, line -> line.addProperty("cause", cause));
        return false;
    }

    public void attach(ServerLevel level, ServerPlayer target, String cause) {

        this.mode = Mode.FOLLOWING;
        this.player = target.getUUID();
        this.playerName = target.getGameProfile().name();
        this.stayAnchor = null;
        this.stayTicks = 0;
        this.settledOn = null;
        this.noticed = true;
        // One quiet sound from where the ghost is, to that player only: the subtitle says "You feel watched".
        target.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.GHOST_WATCHING.get()), SoundSource.AMBIENT,
                this.ghost.getX(), this.ghost.getY(), this.ghost.getZ(), 0.6F, 1.0F, level.getRandom().nextLong()));
        CaptureLog log = CaptureLog.get(level.getServer());
        if (!log.setOutcome(target.getUUID(), this.ghost.getUUID(), "attached")) {
            log.add(target.getUUID(), new CaptureLog.Entry(this.ghost.getUUID(), this.ghost.ghostName(),
                    BuiltInRegistries.ENTITY_TYPE.getKey(this.ghost.getType()).toString(), level.getGameTime() / 24000L, "attached"));
        }
        ServerTrace.ghost("attach_start", this.ghost, line -> {
            line.addProperty("player", this.playerName);
            line.addProperty("cause", cause);
        });
    }

    /** Captured: no more following, and it no longer counts as settled anywhere (the caller reads that first). */
    public void clearForCapture() {

        detach("captured");
        this.settledOn = null;
        this.noticed = false;
    }

    public void detach(String reason) {

        if (this.mode == Mode.NONE) {
            return;
        }
        String name = this.playerName;
        this.mode = Mode.NONE;
        this.player = null;
        this.stayAnchor = null;
        this.stayTicks = 0;
        ServerTrace.ghost("attach_end", this.ghost, line -> {
            line.addProperty("player", name);
            line.addProperty("reason", reason);
        });
    }

    /**
     * Re-homes at {@code pos} and stops following; from now on it haunts there.
     */
    public void settle(ServerLevel level, BlockPos spot) {

        // A respawn point is a bed or an anchor: home is the open space above it, which the ghost can reach.
        BlockPos pos = level.getBlockState(spot).blocksMotion() ? spot.above() : spot;
        UUID owner = this.player;
        String name = this.playerName;
        this.ghost.setHome(level, pos);
        // It was trailing unseen 12–24 blocks back, often further than it can path: it slips straight in.
        this.ghost.getNavigation().stop();
        this.ghost.snapTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, this.ghost.getYRot(), 0.0F);
        this.mode = Mode.NONE;
        this.player = null;
        this.settledOn = owner;
        this.stayAnchor = null;
        this.stayTicks = 0;
        BlockPos coldSpot = this.ghost.getColdSpot();
        ServerTrace.ghost("attach_settle", this.ghost, line -> {
            line.addProperty("player", name);
            line.addProperty("home", pos.toShortString());
            line.addProperty("coldSpot", coldSpot == null ? "none" : coldSpot.toShortString());
        });
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            JournalEvents.fire(player, JournalEvent.GHOST_SETTLED, BuiltInRegistries.ENTITY_TYPE.getKey(this.ghost.getType()).toString());
        }
    }

    /** From {@code GhostEntity.tick}, server side. */
    void tick(ServerLevel level) {

        if (this.ghost.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        if (this.mode == Mode.NONE) {
            noticeIfAgitated(level);
            return;
        }
        ServerPlayer target = this.player == null ? null : level.getServer().getPlayerList().getPlayer(this.player);
        boolean present = target != null && target.level() == level && !target.isSpectator();
        if (this.mode == Mode.FOLLOWING && !present) {
            this.mode = Mode.WAITING;
            ServerTrace.ghost("attach_wait", this.ghost, line -> line.addProperty("player", this.playerName));
            return;
        }
        if (this.mode == Mode.WAITING) {
            if (present && target.distanceTo(this.ghost) <= RESUME_RANGE) {
                this.mode = Mode.FOLLOWING;
                ServerTrace.ghost("attach_resume", this.ghost, line -> line.addProperty("player", this.playerName));
            }
            return;
        }
        // Following.
        BlockPos home = settleSpot(level, target);
        if (home != null) {
            settle(level, home);
            return;
        }
        if (this.ghost.distanceTo(target) > FOLLOW_TELEPORT) {
            Vec3 spot = hiddenSpotBehind(level, target);
            if (spot != null) {
                this.ghost.snapTo(spot.x, spot.y, spot.z, this.ghost.getYRot(), 0.0F);
                this.ghost.getNavigation().stop();
                ServerTrace.ghost("attach_teleport", this.ghost, line -> line.addProperty("player", this.playerName));
            }
        }
    }

    private void noticeIfAgitated(ServerLevel level) {

        if (this.noticed || !this.ghost.isAgitated()) {
            return;
        }
        if (level.getNearestPlayer(this.ghost, Balance.Escape.AGITATED_NOTICE_RANGE) instanceof ServerPlayer near && !near.isSpectator()) {
            this.noticed = true;
            double roll = rollOverride != null ? rollOverride.getAsDouble() : level.getRandom().nextDouble();
            boolean attaches = roll < Balance.Escape.AGITATED_ATTACH_CHANCE;
            ServerTrace.ghost("attach_notice", this.ghost, line -> {
                line.addProperty("player", near.getGameProfile().name());
                line.addProperty("roll", ServerTrace.round(roll));
                line.addProperty("attaches", attaches);
            });
            if (attaches) {
                attach(level, near, "agitated");
            }
        }
    }

    /**
     * Where it settles, if it's time: near the player's respawn point (same dimension), or where they've stood under
     * a roof long enough.
     */
    private @Nullable BlockPos settleSpot(ServerLevel level, ServerPlayer target) {

        ServerPlayer.RespawnConfig respawn = target.getRespawnConfig();
        if (respawn != null) {
            GlobalPos at = respawn.respawnData().globalPos();
            if (at.dimension() == level.dimension() && at.pos().closerToCenterThan(target.position(), Balance.Escape.SETTLE_RESPAWN_RANGE)) {
                return at.pos();
            }
        }
        BlockPos here = target.blockPosition();
        boolean roofed = !level.canSeeSky(here.above());
        if (this.stayAnchor == null || !this.stayAnchor.closerToCenterThan(target.position(), Balance.Escape.SETTLE_STAY_RADIUS) || !roofed) {
            this.stayAnchor = roofed ? here : null;
            this.stayTicks = 0;
            return null;
        }
        this.stayTicks += CHECK_INTERVAL;
        return this.stayTicks >= Balance.Escape.SETTLE_STAY_TICKS ? this.stayAnchor : null;
    }

    /**
     * A spot about {@link #FOLLOW_MAX} behind the player that they can't see: more than the warning angle off their
     * view, or with no line of sight. Null if none of a few tries works.
     */
    private @Nullable Vec3 hiddenSpotBehind(ServerLevel level, ServerPlayer target) {

        Vec3 view = target.getViewVector(1.0F);
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = Math.atan2(-view.x, view.z) + Math.PI + (level.getRandom().nextDouble() - 0.5) * Math.PI / 2;
            double distance = FOLLOW_MIN + level.getRandom().nextDouble() * (FOLLOW_MAX - FOLLOW_MIN);
            double x = target.getX() - Mth.sin((float) angle) * distance;
            double z = target.getZ() + Mth.cos((float) angle) * distance;
            BlockPos column = BlockPos.containing(x, target.getY() + 1, z);
            if (!level.getBlockState(column).isAir() || !level.getBlockState(column.above()).isAir()) {
                continue;
            }
            Vec3 spot = new Vec3(x, column.getY(), z);
            if (WispWarnings.unseen(view, spot.subtract(target.getEyePosition()), true)) {
                return spot;
            }
        }
        return null;
    }

    void save(ValueOutput output) {

        output.putString("AttachMode", this.mode.name());
        if (this.player != null) {
            output.store("AttachPlayer", UUIDUtil.CODEC, this.player);
            output.putString("AttachPlayerName", this.playerName);
        }
        output.putBoolean("AttachNoticed", this.noticed);
        if (this.settledOn != null) {
            output.store("SettledOn", UUIDUtil.CODEC, this.settledOn);
        }
    }

    void load(ValueInput input) {

        try {
            this.mode = Mode.valueOf(input.getStringOr("AttachMode", "NONE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            this.mode = Mode.NONE;
        }
        this.player = input.read("AttachPlayer", UUIDUtil.CODEC).orElse(null);
        this.playerName = input.getStringOr("AttachPlayerName", "");
        this.noticed = input.getBooleanOr("AttachNoticed", false);
        this.settledOn = input.read("SettledOn", UUIDUtil.CODEC).orElse(null);
        if (this.player == null) {
            this.mode = Mode.NONE;
        }
    }
}
