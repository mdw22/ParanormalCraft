package io.github.mdw22.paranormalcraft.client.containment;

import io.github.mdw22.paranormalcraft.binding.SpiritVesselItem;
import io.github.mdw22.paranormalcraft.containment.ContainmentCharge;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlockEntity;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitMenu;
import io.github.mdw22.paranormalcraft.network.ContainmentStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The Containment Unit screen (placeholder art, drawn with fills): the stored ghosts as rows (name, type, time
 * stored, queue state), the charge bar, processing progress, and Process / Release / Bind buttons for the selected
 * ghost, plus the Vessel slot: Bind for an empty vessel and a selected ghost (with a progress bar under the slot), or
 * Unbind for a bound vessel. Rows come from {@link ContainmentStatePayload}.
 */
public class ContainmentUnitScreen extends AbstractContainerScreen<ContainmentUnitMenu> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 230;
    private static final int ROWS_X = 8;
    private static final int ROWS_Y = 18;
    /** Two full lines of text: the name, then the type and stored time. */
    private static final int ROW_HEIGHT = 18;
    private static final int ROWS_WIDTH = 106;
    private static final int BUTTON_X = 118;
    private static final int BUTTON_WIDTH = 50;
    private static final int PANEL = 0xFF2B2E36;
    private static final int PANEL_DARK = 0xFF1A1C22;
    private static final int SLOT = 0xFF0F1014;
    private static final int SLOT_EDGE = 0xFF4A5058;
    private static final int CHARGE_BAR_Y = 76;
    private static final int CYAN = 0xFF6FE7F0;
    private static final int RED = 0xFFFF4030;
    private static final int TEXT = 0xFFE0E6EA;
    private static final int MUTED = 0xFF8A9198;
    private static final int VIOLET = 0xFFC79CF2;

    /** The rows last sent for the open menu. */
    private static List<ContainmentStatePayload.Row> rows = List.of();
    private static int rowsContainer = -1;

    private int selected = -1;
    private @Nullable String bindHint = "";
    private Button process;
    private Button release;
    private Button bind;

    public ContainmentUnitScreen(ContainmentUnitMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelY = ContainmentUnitMenu.INVENTORY_Y - 11;
    }

    /**
     * Client handler for {@link ContainmentStatePayload}.
     */
    public static void handle(ContainmentStatePayload payload) {

        rows = List.copyOf(payload.rows());
        rowsContainer = payload.containerId();
    }

    private List<ContainmentStatePayload.Row> rows() {

        return rowsContainer == this.menu.containerId ? rows : List.of();
    }

    @Override
    protected void init() {

        super.init();
        int x = this.leftPos + BUTTON_X;
        this.process = addRenderableWidget(Button.builder(Component.translatable("gui.paranormalcraft.containment.process"),
                button -> click(ContainmentUnitMenu.ACTION_PROCESS)).bounds(x, this.topPos + ROWS_Y, BUTTON_WIDTH, 14).build());
        this.release = addRenderableWidget(Button.builder(Component.translatable("gui.paranormalcraft.containment.release"),
                button -> click(ContainmentUnitMenu.ACTION_RELEASE)).bounds(x, this.topPos + ROWS_Y + 16, BUTTON_WIDTH, 14).build());
        this.bind = addRenderableWidget(Button.builder(Component.translatable("gui.paranormalcraft.containment.bind"), button -> {
            if (SpiritVesselItem.isBound(this.menu.vessel())) {
                clickAny(ContainmentUnitMenu.ACTION_UNBIND);
            } else {
                click(ContainmentUnitMenu.ACTION_BIND);
            }
        }).bounds(x, this.topPos + ROWS_Y + 32, BUTTON_WIDTH, 14).build());
    }

    private void click(int action) {

        if (this.selected >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, action * 8 + this.selected);
        }
    }

    private void clickAny(int action) {

        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, action * 8);
        }
    }

    /** Debug and tests: selects a ghost row. */
    public void select(int index) {

        this.selected = index;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        int relX = (int) event.x() - this.leftPos - ROWS_X;
        int relY = (int) event.y() - this.topPos - ROWS_Y;
        if (relX >= 0 && relX < ROWS_WIDTH && relY >= 0 && relY < ROW_HEIGHT * 4) {
            int index = relY / ROW_HEIGHT;
            if (index < rows().size()) {
                this.selected = index;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        if (this.selected >= rows().size()) {
            this.selected = rows().size() - 1;
        }
        boolean hasSelection = this.selected >= 0;
        boolean selectionQueued = hasSelection && rows().get(this.selected).queuePosition() >= 0;
        this.process.active = hasSelection && !selectionQueued && !this.menu.unstable();
        boolean selectionBinding = hasSelection && rows().get(this.selected).binding();
        this.release.active = hasSelection && !selectionBinding
                && !(selectionQueued && rows().get(this.selected).queuePosition() == 0 && this.menu.progress() > 0);
        this.process.active = this.process.active && !selectionBinding;
        updateBindButton(hasSelection && !selectionQueued && !selectionBinding);
    }

    private void updateBindButton(boolean selectionFree) {

        ItemStack vessel = this.menu.vessel();
        boolean bound = SpiritVesselItem.isBound(vessel);
        this.bind.setMessage(Component.translatable(bound ? "gui.paranormalcraft.containment.unbind" : "gui.paranormalcraft.containment.bind"));
        String hint;
        if (bound) {
            this.bind.active = this.menu.ghostCount() < ContainmentUnitBlockEntity.CAPACITY;
            hint = this.bind.active ? null : "gui.paranormalcraft.containment.unbind.full";
        } else if (!SpiritVesselItem.isEmptyVessel(vessel)) {
            this.bind.active = false;
            hint = "gui.paranormalcraft.containment.bind.needs_vessel";
        } else if (this.menu.bindProgress() > 0) {
            this.bind.active = false;
            hint = "gui.paranormalcraft.containment.bind.busy";
        } else if (this.menu.charge() < ContainmentUnitBlockEntity.BIND_CHARGE || this.menu.unstable()) {
            this.bind.active = false;
            hint = "gui.paranormalcraft.containment.bind.needs_charge";
        } else {
            this.bind.active = selectionFree;
            hint = selectionFree ? null : "gui.paranormalcraft.containment.bind.select";
        }
        if (!java.util.Objects.equals(hint, this.bindHint)) {
            this.bindHint = hint;
            this.bind.setTooltip(hint == null ? null : Tooltip.create(Component.translatable(hint, ContainmentUnitBlockEntity.BIND_CHARGE)));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {

        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, PANEL);
        graphics.fill(x + ROWS_X - 1, y + ROWS_Y - 1, x + ROWS_X + ROWS_WIDTH + 1, y + ROWS_Y + ROW_HEIGHT * 4 + 1, PANEL_DARK);
        List<ContainmentStatePayload.Row> list = rows();
        for (int i = 0; i < list.size() && i < 4; i++) {
            int top = y + ROWS_Y + i * ROW_HEIGHT;
            if (i == this.selected) {
                graphics.fill(x + ROWS_X, top, x + ROWS_X + ROWS_WIDTH, top + ROW_HEIGHT, 0xFF3A4A55);
            }
        }
        // Charge bar.
        int chargeTop = y + CHARGE_BAR_Y;
        graphics.fill(x + BUTTON_X, chargeTop, x + BUTTON_X + BUTTON_WIDTH, chargeTop + 6, SLOT);
        int chargeWidth = BUTTON_WIDTH * this.menu.charge() / ContainmentCharge.MAX_CHARGE;
        graphics.fill(x + BUTTON_X, chargeTop, x + BUTTON_X + chargeWidth, chargeTop + 6, this.menu.unstable() ? RED : CYAN);
        // Processing progress.
        int progressTop = y + ROWS_Y + ROW_HEIGHT * 4 + 2;
        graphics.fill(x + ROWS_X, progressTop, x + ROWS_X + ROWS_WIDTH, progressTop + 3, SLOT);
        graphics.fill(x + ROWS_X, progressTop, x + ROWS_X + ROWS_WIDTH * this.menu.progress() / 1000, progressTop + 3, 0xFF9CF2A0);
        // Bind progress, under the Vessel slot.
        int bindTop = y + ContainmentUnitMenu.VESSEL_Y + 19;
        int bindLeft = x + ContainmentUnitMenu.VESSEL_X - 1;
        graphics.fill(bindLeft, bindTop, bindLeft + 18, bindTop + 3, SLOT);
        graphics.fill(bindLeft, bindTop, bindLeft + 18 * this.menu.bindProgress() / 1000, bindTop + 3, VIOLET);
        for (Slot slot : this.menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT_EDGE);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {

        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, MUTED, false);
        List<ContainmentStatePayload.Row> list = rows();
        if (list.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.paranormalcraft.containment.empty"), ROWS_X + 3, ROWS_Y + 4, MUTED, false);
        }
        for (int i = 0; i < list.size() && i < 4; i++) {
            ContainmentStatePayload.Row row = list.get(i);
            int top = ROWS_Y + i * ROW_HEIGHT + 1;
            String state = row.binding() ? " ✧" : row.queuePosition() == 0 ? " ⚗" : row.queuePosition() > 0 ? " #" + row.queuePosition() : "";
            graphics.text(this.font, this.font.plainSubstrByWidth(row.name(), ROWS_WIDTH - 18) + state, ROWS_X + 3, top, TEXT, false);
            // The type takes the whole line except the stored time, which sits right-aligned.
            String time = minutes(row.storedTicks());
            int timeWidth = this.font.width(time);
            String type = Component.translatable(typeKey(row.type())).getString();
            graphics.text(this.font, this.font.plainSubstrByWidth(type, ROWS_WIDTH - 9 - timeWidth), ROWS_X + 3, top + 9, MUTED, false);
            graphics.text(this.font, time, ROWS_X + ROWS_WIDTH - 2 - timeWidth, top + 9, MUTED, false);
        }
        graphics.text(this.font, Component.translatable(this.menu.unstable() ? "gui.paranormalcraft.containment.unstable"
                : "gui.paranormalcraft.containment.charge", this.menu.charge()), BUTTON_X, CHARGE_BAR_Y - 10, this.menu.unstable() ? RED : MUTED, false);
        graphics.text(this.font, Component.translatable("gui.paranormalcraft.containment.trap"), ContainmentUnitMenu.INPUT_X, ContainmentUnitMenu.INPUT_Y - 10, MUTED, false);
        graphics.text(this.font, Component.translatable("gui.paranormalcraft.containment.fuel"), ContainmentUnitMenu.FUEL_X, ContainmentUnitMenu.FUEL_Y - 10, MUTED, false);
        graphics.text(this.font, Component.translatable("gui.paranormalcraft.containment.output"), ContainmentUnitMenu.OUTPUT_X, ContainmentUnitMenu.OUTPUT_Y - 10, MUTED, false);
        graphics.text(this.font, Component.translatable("gui.paranormalcraft.containment.vessel"), ContainmentUnitMenu.VESSEL_X, ContainmentUnitMenu.VESSEL_Y - 10, MUTED, false);
    }

    private static String typeKey(String type) {

        Identifier id = Identifier.parse(type);
        return "entity." + id.getNamespace() + "." + id.getPath();
    }

    private static String minutes(long ticks) {

        long seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /** For tests: whether the screen has rows for its menu. */
    public static int rowCount() {

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gui.screen() instanceof ContainmentUnitScreen screen ? screen.rows().size() : 0;
    }
}
