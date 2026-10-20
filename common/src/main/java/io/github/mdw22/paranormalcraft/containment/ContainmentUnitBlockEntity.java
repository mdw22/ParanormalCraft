package io.github.mdw22.paranormalcraft.containment;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.binding.BoundJobs;
import io.github.mdw22.paranormalcraft.binding.SpiritVesselItem;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.CapturedGhosts;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Presence;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.journal.JournalEvent;
import io.github.mdw22.paranormalcraft.journal.JournalEvents;
import io.github.mdw22.paranormalcraft.network.ContainmentStatePayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * A Containment Unit: holds up to {@link #CAPACITY} ghosts, fuelled by soul sand or soul soil.
 * <ul>
 *     <li><b>Slots:</b> {@link #INPUT_SLOT} takes a Filled Ghost Trap (its ghost moves into storage, the empty trap goes
 *     to the output), {@link #FUEL_SLOT} takes soul sand or soul soil, and {@link #OUTPUT_START} to
 *     {@link #OUTPUT_END} hold ectoplasm, residue and empty traps. Hoppers: fuel from the top or sides, traps from the
 *     sides, outputs from the bottom.</li>
 *     <li><b>Charge</b> ({@link ContainmentCharge}): drains with every stored ghost and while processing; at 0 with
 *     ghosts inside the unit is Unstable (alarm, red glow, no processing).</li>
 *     <li><b>Process</b> consumes the selected ghost over {@link #PROCESS_TICKS} for typed ectoplasm
 *     ({@link ContainmentTables}); one at a time, the rest queue in the order chosen.</li>
 *     <li><b>Release</b> sets the ghost free (it rises and fades for everyone) and blesses the releasing player.</li>
 *     <li><b>Bind</b> moves the selected ghost into the empty Spirit Vessel in {@link #VESSEL_SLOT} over
 *     {@link #BIND_TICKS} for {@link #BIND_CHARGE}; <b>Unbind</b> puts a bound vessel's ghost back into storage.</li>
 * </ul>
 * Breaking the unit drops every stored ghost as a Filled Ghost Trap (never a loose ghost) and its slot contents.
 */
public class ContainmentUnitBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int CAPACITY = Balance.Containment.CAPACITY;
    public static final int PROCESS_TICKS = Balance.Containment.PROCESS_TICKS;
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_END = 7;
    public static final int VESSEL_SLOT = 8;
    public static final int SLOTS = VESSEL_SLOT + 1;
    public static final int BIND_TICKS = Balance.Binding.BIND_TICKS;
    public static final int BIND_CHARGE = Balance.Binding.BIND_CHARGE;
    private static final int[] TOP_SLOTS = {FUEL_SLOT};
    private static final int[] SIDE_SLOTS = {INPUT_SLOT, FUEL_SLOT};
    private static final int[] BOTTOM_SLOTS = IntStream.rangeClosed(OUTPUT_START, OUTPUT_END).toArray();

    /** Menu data indexes (synced as shorts). */
    public static final int DATA_CHARGE = 0;
    public static final int DATA_PROCESS = 1;
    public static final int DATA_UNSTABLE = 2;
    public static final int DATA_COUNT = 3;
    public static final int DATA_PROCESSING_INDEX = 4;
    /** Bind progress, 0 to 1000. */
    public static final int DATA_BIND = 5;
    public static final int DATA_SIZE = 6;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final List<StoredGhost> stored = new ArrayList<>();
    /** Ghost ids in the order they were chosen for processing; the first is the one processing. */
    private final List<UUID> queue = new ArrayList<>();
    private ContainmentCharge charge = ContainmentCharge.EMPTY;
    private int processTicks;
    private boolean unstable;
    /** The last player to open the menu, credited with loads made by automation. */
    private @Nullable UUID lastUser;
    private int lastChargeBand = -1;
    /** The ghost being bound into the vessel, and how far along. */
    private @Nullable UUID binding;
    private int bindTicks;
    private @Nullable UUID bindBy;

    private final ContainerData data = new ContainerData() {

        @Override
        public int get(int index) {

            return switch (index) {
                case DATA_CHARGE -> ContainmentUnitBlockEntity.this.charge.charge();
                case DATA_PROCESS -> ContainmentUnitBlockEntity.this.processTicks * 1000 / PROCESS_TICKS;
                case DATA_UNSTABLE -> ContainmentUnitBlockEntity.this.unstable ? 1 : 0;
                case DATA_COUNT -> ContainmentUnitBlockEntity.this.stored.size();
                case DATA_PROCESSING_INDEX -> processingIndex();
                case DATA_BIND -> ContainmentUnitBlockEntity.this.binding == null ? 0
                        : ContainmentUnitBlockEntity.this.bindTicks * 1000 / BIND_TICKS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {

            return DATA_SIZE;
        }
    };

    public ContainmentUnitBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlocks.CONTAINMENT_UNIT_ENTITY.get(), pos, state);
    }

    // ---- state ----

    public List<StoredGhost> stored() {

        return List.copyOf(this.stored);
    }

    public List<UUID> queue() {

        return List.copyOf(this.queue);
    }

    public int charge() {

        return this.charge.charge();
    }

    public int processTicks() {

        return this.processTicks;
    }

    public boolean isUnstable() {

        return this.unstable;
    }

    /** Index in {@link #stored()} of the ghost being processed, or -1. */
    public int processingIndex() {

        if (this.queue.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < this.stored.size(); i++) {
            if (this.stored.get(i).id().equals(this.queue.getFirst())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Debug, scripts and tests: moves the ghost being processed this many ticks closer to done, stopping one tick short
     * so it finishes through the normal path on the next tick.
     */
    public int debugAdvanceProcessing(int ticks) {

        if (this.queue.isEmpty()) {
            return 0;
        }
        this.processTicks = Math.min(PROCESS_TICKS - 1, this.processTicks + ticks);
        setChanged();
        return this.processTicks;
    }

    /** Debug: sets the charge meter. */
    public void setCharge(int value) {

        this.charge = this.charge.withCharge(value);
        setChanged();
    }

    // ---- ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, ContainmentUnitBlockEntity unit) {

        if (level instanceof ServerLevel serverLevel) {
            unit.tick(serverLevel, pos, state);
        }
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {

        tryLoad(level, pos);
        if (this.charge.wantsFuel() && isFuel(this.items.get(FUEL_SLOT))) {
            this.items.get(FUEL_SLOT).shrink(1);
            this.charge = this.charge.fuelled();
            setChanged();
        }
        boolean processing = !this.queue.isEmpty() && !this.unstable;
        int before = this.charge.charge();
        this.charge = this.charge.tick(this.stored.size(), processing);
        if (this.charge.charge() != before) {
            setChanged();
        }
        traceChargeBand();
        boolean nowUnstable = this.charge.unstable(this.stored.size());
        if (nowUnstable != this.unstable) {
            this.unstable = nowUnstable;
            ServerTrace.event(nowUnstable ? "containment_unstable_start" : "containment_unstable_end", line -> {
                line.addProperty("pos", pos.toShortString());
                line.addProperty("ghosts", this.stored.size());
            });
            setChanged();
        }
        if (processing) {
            tickProcessing(level, pos);
        }
        if (this.binding != null && !this.unstable) {
            tickBinding(level, pos);
        }
        sounds(level, pos);
        updateBlockState(level, pos, state);
    }

    private void tryLoad(ServerLevel level, BlockPos pos) {

        ItemStack input = this.items.get(INPUT_SLOT);
        CapturedGhost ghost = input.get(ModDataComponents.CAPTURED_GHOST.get());
        if (ghost == null || this.stored.size() >= CAPACITY || !canOutput(new ItemStack(ModItems.GHOST_TRAP.get()))) {
            return;
        }
        input.shrink(1);
        output(new ItemStack(ModItems.GHOST_TRAP.get()));
        StoredGhost entry = new StoredGhost(ghost, level.getGameTime(), Optional.empty());
        this.stored.add(entry);
        level.playSound(null, pos, ModSounds.CONTAINMENT_LOAD.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        ServerTrace.event("containment_load", line -> {
            line.addProperty("pos", pos.toShortString());
            line.addProperty("name", ghost.name());
            line.addProperty("ghostType", ghost.entityType());
            line.addProperty("stored", this.stored.size());
        });
        ServerPlayer user = creditedPlayer(level, pos);
        if (user != null) {
            JournalEvents.fire(user, JournalEvent.CONTAINED, ghost.entityType());
        }
        setChanged();
    }

    private void tickProcessing(ServerLevel level, BlockPos pos) {

        int index = processingIndex();
        if (index < 0) {
            this.queue.removeFirst();
            this.processTicks = 0;
            return;
        }
        StoredGhost ghost = this.stored.get(index);
        if (this.processTicks == 0) {
            ServerTrace.event("containment_process_start", line -> {
                line.addProperty("pos", pos.toShortString());
                line.addProperty("name", ghost.ghost().name());
            });
        }
        if (this.processTicks < PROCESS_TICKS) {
            this.processTicks++;
            setChanged();
        }
        if (this.processTicks >= PROCESS_TICKS) {
            finishProcessing(level, pos, index);
        }
    }

    /**
     * The ghost's yield goes to the output. If the output can't take it all, it waits there, done, until it can.
     */
    private void finishProcessing(ServerLevel level, BlockPos pos, int index) {

        StoredGhost ghost = this.stored.get(index);
        List<ItemStack> yields = rollYields(level, ghost.ghost().entityType());
        for (ItemStack stack : yields) {
            if (!canOutput(stack)) {
                return;
            }
        }
        yields.forEach(this::output);
        this.stored.remove(index);
        this.queue.removeFirst();
        this.processTicks = 0;
        String outcome = yields.stream().map(stack -> stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
                .reduce((a, b) -> a + ", " + b).orElse("nothing");
        ServerTrace.event("containment_process_done", line -> {
            line.addProperty("pos", pos.toShortString());
            line.addProperty("name", ghost.ghost().name());
            line.addProperty("yield", outcome);
        });
        level.playSound(null, pos, ModSounds.CONTAINMENT_BUBBLE.get(), SoundSource.BLOCKS, 1.0F, 0.7F);
        CaptureLog.get(level.getServer()).setOutcome(ghost.id(), "processed");
        ghost.queuedBy().map(id -> level.getServer().getPlayerList().getPlayer(id))
                .ifPresent(player -> JournalEvents.fire(player, JournalEvent.PROCESSED, ghost.ghost().entityType()));
        setChanged();
    }

    /** Tests set this to fix the yield and residue rolls; null rolls for real. */
    private java.util.function.@Nullable DoubleSupplier rollOverride;

    /**
     * Tests only: the value this unit's processing rolls use (yield count and residue chance).
     */
    public void setRollOverride(java.util.function.@Nullable DoubleSupplier roll) {

        this.rollOverride = roll;
    }

    private List<ItemStack> rollYields(ServerLevel level, String ghostType) {

        List<ItemStack> yields = new ArrayList<>();
        ContainmentTables.Yield yield = ContainmentTables.yieldFor(ghostType);
        if (yield != null) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(yield.item()));
            double roll = this.rollOverride != null ? this.rollOverride.getAsDouble() : level.getRandom().nextDouble();
            yields.add(new ItemStack(item, ContainmentTables.yieldCount(yield, roll)));
        }
        double residueRoll = this.rollOverride != null ? this.rollOverride.getAsDouble() : level.getRandom().nextDouble();
        if (ContainmentTables.residue(residueRoll)) {
            yields.add(new ItemStack(ModItems.SPECTRAL_RESIDUE.get()));
        }
        return yields;
    }

    private void traceChargeBand() {

        int band = this.charge.charge() / 25;
        if (band != this.lastChargeBand) {
            int charge = this.charge.charge();
            this.lastChargeBand = band;
            ServerTrace.event("containment_charge", line -> {
                line.addProperty("pos", this.worldPosition.toShortString());
                line.addProperty("charge", charge);
            });
        }
    }

    private void sounds(ServerLevel level, BlockPos pos) {

        long time = level.getGameTime();
        int ghosts = this.stored.size();
        if (this.unstable && time % Balance.Containment.ALARM_INTERVAL_TICKS == 0) {
            level.playSound(null, pos, ModSounds.CONTAINMENT_ALARM.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (ghosts > 0 && time % Balance.Containment.HUM_INTERVAL_TICKS == 0) {
            level.playSound(null, pos, ModSounds.CONTAINMENT_HUM.get(), SoundSource.BLOCKS, 0.2F + 0.15F * ghosts, 0.6F);
        }
        if (!this.queue.isEmpty() && !this.unstable && time % Balance.Containment.BUBBLE_INTERVAL_TICKS == 0) {
            level.playSound(null, pos, ModSounds.CONTAINMENT_BUBBLE.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
        }
    }

    private void updateBlockState(ServerLevel level, BlockPos pos, BlockState state) {

        BlockState wanted = state.setValue(ContainmentUnitBlock.GHOSTS, Math.min(CAPACITY, this.stored.size()))
                .setValue(ContainmentUnitBlock.UNSTABLE, this.unstable);
        if (wanted != state) {
            level.setBlock(pos, wanted, Block.UPDATE_ALL);
        }
    }

    private @Nullable ServerPlayer creditedPlayer(ServerLevel level, BlockPos pos) {

        if (this.lastUser != null) {
            ServerPlayer user = level.getServer().getPlayerList().getPlayer(this.lastUser);
            if (user != null) {
                return user;
            }
        }
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8.0, false);
        return nearest instanceof ServerPlayer player ? player : null;
    }

    // ---- binding ----

    public @Nullable UUID binding() {

        return this.binding;
    }

    public int bindTicks() {

        return this.bindTicks;
    }

    private int indexOf(UUID id) {

        for (int i = 0; i < this.stored.size(); i++) {
            if (this.stored.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Starts binding the ghost at {@code index} into the empty Spirit Vessel in {@link #VESSEL_SLOT}: takes
     * {@link #BIND_CHARGE} now and {@link #BIND_TICKS} to finish. The ghost can't be queued, processed or bound
     * already, its type needs a job ({@link BoundJobs}), and the unit can't be Unstable.
     */
    public boolean bind(int index, ServerPlayer player) {

        if (index < 0 || index >= this.stored.size() || this.binding != null || this.unstable
                || !SpiritVesselItem.isEmptyVessel(this.items.get(VESSEL_SLOT)) || this.charge.charge() < BIND_CHARGE) {
            return false;
        }
        StoredGhost ghost = this.stored.get(index);
        if (this.queue.contains(ghost.id()) || !BoundJobs.canBind(ghost.ghost().entityType())) {
            return false;
        }
        this.charge = this.charge.withCharge(this.charge.charge() - BIND_CHARGE);
        this.binding = ghost.id();
        this.bindTicks = 0;
        this.bindBy = player.getUUID();
        ServerTrace.event("bind_start", line -> {
            line.addProperty("pos", this.worldPosition.toShortString());
            line.addProperty("name", ghost.ghost().name());
            line.addProperty("player", player.getGameProfile().name());
            line.addProperty("charge", this.charge.charge());
        });
        setChanged();
        return true;
    }

    private void tickBinding(ServerLevel level, BlockPos pos) {

        int index = this.binding == null ? -1 : indexOf(this.binding);
        if (index < 0 || !SpiritVesselItem.isEmptyVessel(this.items.get(VESSEL_SLOT))) {
            String name = index < 0 ? "?" : this.stored.get(index).ghost().name();
            ServerTrace.event("bind_cancel", line -> {
                line.addProperty("pos", pos.toShortString());
                line.addProperty("name", name);
            });
            this.binding = null;
            this.bindTicks = 0;
            setChanged();
            return;
        }
        this.bindTicks++;
        if (this.bindTicks % Balance.Binding.TONE_INTERVAL_TICKS == 0) {
            float progress = (float) this.bindTicks / BIND_TICKS;
            float pitch = Balance.Binding.TONE_PITCH_START + (Balance.Binding.TONE_PITCH_END - Balance.Binding.TONE_PITCH_START) * progress;
            level.playSound(null, pos, ModSounds.CONTAINMENT_BIND_TONE.get(), SoundSource.BLOCKS, 0.6F, pitch);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.15, 0.1, 0.15, 0.005);
        }
        if (this.bindTicks >= BIND_TICKS) {
            finishBinding(level, pos, index);
        }
        setChanged();
    }

    private void finishBinding(ServerLevel level, BlockPos pos, int index) {

        StoredGhost ghost = this.stored.remove(index);
        this.queue.remove(ghost.id());
        ItemStack vessel = this.items.get(VESSEL_SLOT);
        vessel.set(ModDataComponents.CAPTURED_GHOST.get(), ghost.ghost());
        this.binding = null;
        this.bindTicks = 0;
        level.playSound(null, pos, ModSounds.CONTAINMENT_BIND_DONE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        BoundJobs.Job job = BoundJobs.jobFor(ghost.ghost().entityType());
        ServerTrace.event("bind_done", line -> {
            line.addProperty("pos", pos.toShortString());
            line.addProperty("name", ghost.ghost().name());
            line.addProperty("job", job == null ? "none" : job.id());
        });
        CaptureLog.get(level.getServer()).setOutcome(ghost.id(), "bound");
        UUID by = this.bindBy;
        this.bindBy = null;
        ServerPlayer player = by == null ? null : level.getServer().getPlayerList().getPlayer(by);
        if (player != null) {
            JournalEvents.fire(player, JournalEvent.BOUND, ghost.ghost().entityType());
        }
        setChanged();
    }

    /**
     * Tests: moves a bind this many ticks closer to done, stopping one tick short so it finishes normally.
     */
    public int debugAdvanceBinding(int ticks) {

        if (this.binding == null) {
            return 0;
        }
        this.bindTicks = Math.min(BIND_TICKS - 1, this.bindTicks + ticks);
        setChanged();
        return this.bindTicks;
    }

    /**
     * Debug and scripts: binds the ghost at {@code index} at once. Puts an empty vessel in the slot if it is empty;
     * charge is not needed.
     */
    public boolean debugBind(ServerLevel level, int index, ServerPlayer player) {

        if (this.items.get(VESSEL_SLOT).isEmpty()) {
            this.items.set(VESSEL_SLOT, new ItemStack(ModItems.SPIRIT_VESSEL.get()));
        }
        if (this.charge.charge() < BIND_CHARGE) {
            this.charge = this.charge.withCharge(BIND_CHARGE);
        }
        if (!bind(index, player)) {
            return false;
        }
        finishBinding(level, this.worldPosition, indexOf(this.binding));
        return true;
    }

    /**
     * Puts the ghost in the bound vessel in {@link #VESSEL_SLOT} back into storage (if there is room) and empties the
     * vessel.
     */
    public boolean unbind(ServerPlayer player) {

        ItemStack vessel = this.items.get(VESSEL_SLOT);
        CapturedGhost ghost = SpiritVesselItem.isBound(vessel) ? vessel.get(ModDataComponents.CAPTURED_GHOST.get()) : null;
        if (ghost == null || this.stored.size() >= CAPACITY || !(this.level instanceof ServerLevel level)) {
            return false;
        }
        vessel.remove(ModDataComponents.CAPTURED_GHOST.get());
        this.stored.add(new StoredGhost(ghost, level.getGameTime(), Optional.empty()));
        level.playSound(null, this.worldPosition, ModSounds.CONTAINMENT_LOAD.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
        CaptureLog.get(level.getServer()).setOutcome(ghost.id(), "stored");
        ServerTrace.event("unbind", line -> {
            line.addProperty("pos", this.worldPosition.toShortString());
            line.addProperty("name", ghost.name());
            line.addProperty("player", player.getGameProfile().name());
            line.addProperty("stored", this.stored.size());
        });
        setChanged();
        return true;
    }

    // ---- actions from the menu ----

    /**
     * Queues the ghost at {@code index} for processing (no-op if it already is).
     */
    public boolean queueProcess(int index, ServerPlayer player) {

        if (index < 0 || index >= this.stored.size()) {
            return false;
        }
        StoredGhost ghost = this.stored.get(index);
        if (this.queue.contains(ghost.id()) || ghost.id().equals(this.binding)) {
            return false;
        }
        this.stored.set(index, ghost.queued(player.getUUID()));
        this.queue.add(ghost.id());
        ServerTrace.event("containment_queue", line -> {
            line.addProperty("name", ghost.ghost().name());
            line.addProperty("position", this.queue.size());
        });
        setChanged();
        return true;
    }

    /**
     * Sets the ghost at {@code index} free above the unit and blesses {@code player}. A ghost being processed can't
     * be released.
     */
    public boolean release(int index, ServerPlayer player) {

        if (index < 0 || index >= this.stored.size() || index == processingIndex() && this.processTicks > 0 || !(this.level instanceof ServerLevel level)
                || this.stored.get(index).id().equals(this.binding)) {
            return false;
        }
        StoredGhost ghost = this.stored.remove(index);
        this.queue.remove(ghost.id());
        BlockPos pos = this.worldPosition;
        GhostEntity released = recreate(level, ghost.ghost(), pos);
        if (released != null) {
            released.startAscending();
        }
        level.playSound(null, pos, ModSounds.GHOST_ASCEND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        ContainmentTables.Blessing blessing = ContainmentTables.blessingFor(ghost.ghost().entityType());
        if (blessing != null) {
            Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(blessing.effect()));
            // Ambient, no particles: a 3-day effect's swirl right next to the camera would cover the screen. The HUD icon stays.
            effect.ifPresent(holder -> player.addEffect(new MobEffectInstance(holder, blessing.ticks(), 0, true, false, true)));
        }
        ServerTrace.event("containment_release", line -> {
            line.addProperty("pos", pos.toShortString());
            line.addProperty("name", ghost.ghost().name());
            line.addProperty("player", player.getGameProfile().name());
            line.addProperty("blessing", blessing == null ? "none" : blessing.effect());
        });
        CaptureLog.get(level.getServer()).setOutcome(ghost.id(), "released");
        JournalEvents.fire(player, JournalEvent.RELEASED, ghost.ghost().entityType());
        setChanged();
        return true;
    }

    private static @Nullable GhostEntity recreate(ServerLevel level, CapturedGhost captured, BlockPos pos) {

        CompoundTag data = captured.entity().copy();
        data.putFloat("Presence", Presence.MAX);
        Entity entity;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            entity = EntityType.loadEntityRecursive(TagValueInput.create(reporter, level.registryAccess(), data), level, EntitySpawnReason.LOAD, loaded -> loaded);
        }
        if (!(entity instanceof GhostEntity ghost)) {
            return null;
        }
        ghost.snapTo(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, ghost.getYRot(), 0.0F);
        level.addFreshEntity(ghost);
        return ghost;
    }

    /**
     * Debug and tests: stores {@code count} freshly made Lingering Spirits (homed at the unit), up to capacity.
     */
    public int fillForDebug(ServerLevel level, int count) {

        int added = 0;
        while (added < count && this.stored.size() < CAPACITY) {
            CapturedGhost ghost = CapturedGhosts.fresh(level, this.worldPosition.above(), "debug");
            if (ghost == null) {
                break;
            }
            this.stored.add(new StoredGhost(ghost, level.getGameTime(), Optional.empty()));
            added++;
        }
        setChanged();
        return added;
    }

    /**
     * The menu's row data for the client: name, type and stored time for each ghost, and its place in the queue.
     */
    public ContainmentStatePayload statePayload(int containerId) {

        long now = this.level == null ? 0 : this.level.getGameTime();
        List<ContainmentStatePayload.Row> rows = new ArrayList<>();
        for (StoredGhost ghost : this.stored) {
            rows.add(new ContainmentStatePayload.Row(ghost.ghost().name(), ghost.ghost().entityType(), now - ghost.storedAt(),
                    this.queue.indexOf(ghost.id()), ghost.id().equals(this.binding)));
        }
        return new ContainmentStatePayload(containerId, rows);
    }

    public void sendState(ServerPlayer player, int containerId) {

        Services.NETWORK.sendToPlayer(player, statePayload(containerId));
    }

    // ---- items ----

    public static boolean isFuel(ItemStack stack) {

        return stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL);
    }

    public static boolean isLoadable(ItemStack stack) {

        return stack.is(ModItems.FILLED_GHOST_TRAP.get()) && stack.has(ModDataComponents.CAPTURED_GHOST.get());
    }

    private boolean canOutput(ItemStack stack) {

        int remaining = stack.getCount();
        for (int slot = OUTPUT_START; slot <= OUTPUT_END && remaining > 0; slot++) {
            ItemStack existing = this.items.get(slot);
            if (existing.isEmpty()) {
                remaining -= stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                remaining -= existing.getMaxStackSize() - existing.getCount();
            }
        }
        return remaining <= 0;
    }

    private void output(ItemStack stack) {

        ItemStack rest = stack.copy();
        for (int slot = OUTPUT_START; slot <= OUTPUT_END && !rest.isEmpty(); slot++) {
            ItemStack existing = this.items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, rest)) {
                int move = Math.min(rest.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(move);
                rest.shrink(move);
            }
        }
        for (int slot = OUTPUT_START; slot <= OUTPUT_END && !rest.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, rest.copy());
                rest = ItemStack.EMPTY;
            }
        }
    }

    @Override
    protected Component getDefaultName() {

        return Component.translatable("container.paranormalcraft.containment_unit");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {

        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {

        this.items = items;
    }

    @Override
    public int getContainerSize() {

        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {

        return switch (slot) {
            case INPUT_SLOT -> isLoadable(stack);
            case FUEL_SLOT -> isFuel(stack);
            case VESSEL_SLOT -> stack.getItem() instanceof SpiritVesselItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {

        return switch (side) {
            case UP -> TOP_SLOTS;
            case DOWN -> BOTTOM_SLOTS;
            default -> SIDE_SLOTS;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {

        if (side == Direction.DOWN) {
            return false;
        }
        if (side == Direction.UP) {
            return slot == FUEL_SLOT && isFuel(stack);
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {

        return side == Direction.DOWN && slot >= OUTPUT_START && slot <= OUTPUT_END;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {

        this.lastUser = inventory.player.getUUID();
        return new ContainmentUnitMenu(containerId, inventory, this, this.data);
    }

    /**
     * Stored ghosts drop as Filled Ghost Traps, then the slots drop as usual.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {

        if (this.level != null) {
            for (StoredGhost ghost : this.stored) {
                ItemStack trap = new ItemStack(ModItems.FILLED_GHOST_TRAP.get());
                trap.set(ModDataComponents.CAPTURED_GHOST.get(), ghost.ghost());
                Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, trap);
            }
            this.stored.clear();
            this.queue.clear();
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setLevel(Level level) {

        super.setLevel(level);
        if (!level.isClientSide()) {
            LoadedPositions.CONTAINMENT_UNITS.add(level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {

        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            LoadedPositions.CONTAINMENT_UNITS.remove(this.level, this.worldPosition);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.stored.clear();
        input.read("Stored", StoredGhost.CODEC.listOf()).ifPresent(this.stored::addAll);
        this.queue.clear();
        input.read("Queue", net.minecraft.core.UUIDUtil.CODEC.listOf()).ifPresent(this.queue::addAll);
        this.charge = input.read("Charge", ContainmentCharge.CODEC).orElse(ContainmentCharge.EMPTY);
        this.processTicks = input.getIntOr("ProcessTicks", 0);
        this.unstable = input.getBooleanOr("Unstable", false);
        this.lastUser = input.read("LastUser", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        this.binding = input.read("Binding", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        this.bindTicks = input.getIntOr("BindTicks", 0);
        this.bindBy = input.read("BindBy", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.store("Stored", StoredGhost.CODEC.listOf(), this.stored);
        output.store("Queue", net.minecraft.core.UUIDUtil.CODEC.listOf(), this.queue);
        output.store("Charge", ContainmentCharge.CODEC, this.charge);
        output.putInt("ProcessTicks", this.processTicks);
        output.putBoolean("Unstable", this.unstable);
        if (this.lastUser != null) {
            output.store("LastUser", net.minecraft.core.UUIDUtil.CODEC, this.lastUser);
        }
        if (this.binding != null) {
            output.store("Binding", net.minecraft.core.UUIDUtil.CODEC, this.binding);
            output.putInt("BindTicks", this.bindTicks);
        }
        if (this.bindBy != null) {
            output.store("BindBy", net.minecraft.core.UUIDUtil.CODEC, this.bindBy);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {

        super.removeComponentsFromTag(output);
    }
}
