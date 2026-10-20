package io.github.mdw22.paranormalcraft.client.journal;

import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.journal.JournalTypes;
import io.github.mdw22.paranormalcraft.network.JournalPayload;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEffects;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The Field Journal, read-only, as two-page spreads (placeholder art, drawn with fills, vanilla font size):
 * <ul>
 *     <li><b>Field Notes:</b> a diagram of icons and arrows, Find → Weaken → Catch → Contain, then Process, Release
 *     or Bind.</li>
 *     <li><b>One spread per ghost type met:</b> on the left a slowly turning portrait (a client-only dummy entity,
 *     drawn as a dark silhouette until the player has seen one) over the facts rows from {@link JournalTypes}
 *     ("?" until unlocked); on the right the short stage notes ("?" until reached), plus the bound-job line once one
 *     has been bound.</li>
 *     <li><b>Captures:</b> compact rows: type icon, name, day, outcome icon.</li>
 * </ul>
 * Every icon shows its full name on hover. All text is lang ({@code journal.paranormalcraft.*}).
 */
public class JournalScreen extends Screen {

    private static final int PAGE_WIDTH = 196;
    private static final int PAGE_HEIGHT = 200;
    private static final int SPINE = 4;
    private static final int SPREAD_WIDTH = PAGE_WIDTH * 2 + SPINE;
    private static final int MARGIN = 12;
    private static final int PAPER = 0xFFE9DFC4;
    private static final int PAPER_EDGE = 0xFF8C7A55;
    private static final int SPINE_COLOUR = 0xFF5E4E34;
    private static final int INK = 0xFF2E2618;
    private static final int FADED = 0xFF8C7C5E;
    private static final int LINE = 0xFF7A6A4C;
    private static final int PORTRAIT_BACK = 0x22301E08;
    private static final int CAPTURES_PER_PAGE = 8;
    private static final int CAPTURE_ROW = 20;
    private static final int FACT_ROW = 17;
    private static final int PORTRAIT_ID_BASE = -1_000_000;
    /** Milliseconds per portrait turn. */
    private static final long TURN_MILLIS = 9000;

    /** A spread: Field Notes, one ghost type, or a page pair of captures. */
    private sealed interface Spread permits Notes, TypePage, Captures {
    }

    private record Notes() implements Spread {
    }

    private record TypePage(String type, int stage, boolean bound) implements Spread {
    }

    private record Captures(List<CaptureLog.Entry> left, List<CaptureLog.Entry> right) implements Spread {
    }

    private final List<Spread> spreads;
    private final Map<String, GhostEntity> portraits = new HashMap<>();
    private int spread;
    private Button previous;
    private Button next;
    private int mouseX;
    private int mouseY;

    public JournalScreen(JournalPayload payload) {

        super(Component.translatable("item.paranormalcraft.field_journal"));
        this.spreads = buildSpreads(payload);
    }

    /**
     * Client handler for {@link JournalPayload}: opens the journal.
     */
    public static void open(JournalPayload payload) {

        JournalScreen screen = new JournalScreen(payload);
        ClientTrace.event("journal_screen", line -> {
            line.addProperty("spreads", screen.spreads.size());
            payload.stages().forEach(stage -> line.addProperty(stage.type(), stage.stage()));
        });
        Minecraft.getInstance().gui.setScreen(screen);
    }

    private static List<Spread> buildSpreads(JournalPayload payload) {

        List<Spread> spreads = new ArrayList<>();
        spreads.add(new Notes());
        for (JournalPayload.TypeStage stage : payload.stages()) {
            if (stage.stage() > 0) {
                spreads.add(new TypePage(stage.type(), stage.stage(), stage.bound()));
            }
        }
        List<CaptureLog.Entry> captures = payload.captures();
        if (captures.isEmpty()) {
            spreads.add(new Captures(List.of(), List.of()));
        }
        for (int start = 0; start < captures.size(); start += CAPTURES_PER_PAGE * 2) {
            int middle = Math.min(captures.size(), start + CAPTURES_PER_PAGE);
            spreads.add(new Captures(captures.subList(start, middle), captures.subList(middle, Math.min(captures.size(), middle + CAPTURES_PER_PAGE))));
        }
        return spreads;
    }

    /** Spreads, for tests and the debug command (kept as "pages" for older scripts). */
    public int pageCount() {

        return this.spreads.size();
    }

    public int page() {

        return this.spread;
    }

    public void setPage(int page) {

        this.spread = Math.clamp(page, 0, this.spreads.size() - 1);
        updateButtons();
    }

    /** The spread index of the Field Notes (0), a ghost type's page, or the first captures spread; -1 if none. */
    public int spreadOf(String kind) {

        for (int i = 0; i < this.spreads.size(); i++) {
            Spread spread = this.spreads.get(i);
            boolean match = switch (spread) {
                case Notes _ -> kind.equals("notes");
                case TypePage page -> page.type().equals(kind);
                case Captures _ -> kind.equals("captures");
            };
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private int left() {

        return (this.width - SPREAD_WIDTH) / 2;
    }

    private int top() {

        return Math.max(4, (this.height - PAGE_HEIGHT - 24) / 2);
    }

    @Override
    protected void init() {

        int buttonY = top() + PAGE_HEIGHT + 4;
        this.previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> setPage(this.spread - 1))
                .bounds(left(), buttonY, 20, 20).build());
        this.next = addRenderableWidget(Button.builder(Component.literal(">"), button -> setPage(this.spread + 1))
                .bounds(left() + SPREAD_WIDTH - 20, buttonY, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left() + SPREAD_WIDTH / 2 - 40, buttonY, 80, 20).build());
        updateButtons();
    }

    private void updateButtons() {

        if (this.previous != null) {
            this.previous.active = this.spread > 0;
            this.next.active = this.spread < this.spreads.size() - 1;
        }
    }

    @Override
    public void tick() {

        // The dummies never join the level; ticking them here keeps their idle animation going.
        this.portraits.values().forEach(ghost -> ghost.tickCount++);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        int x = left();
        int y = top();
        graphics.fill(x - 2, y - 2, x + SPREAD_WIDTH + 2, y + PAGE_HEIGHT + 2, PAPER_EDGE);
        graphics.fill(x, y, x + PAGE_WIDTH, y + PAGE_HEIGHT, PAPER);
        graphics.fill(x + PAGE_WIDTH + SPINE, y, x + SPREAD_WIDTH, y + PAGE_HEIGHT, PAPER);
        graphics.fill(x + PAGE_WIDTH, y - 2, x + PAGE_WIDTH + SPINE, y + PAGE_HEIGHT + 2, SPINE_COLOUR);
        int rightPage = x + PAGE_WIDTH + SPINE;
        switch (this.spreads.get(this.spread)) {
            case Notes _ -> drawNotes(graphics, x, rightPage, y);
            case TypePage page -> drawType(graphics, page, x, rightPage, y);
            case Captures captures -> drawCaptures(graphics, captures, x, rightPage, y);
        }
        // Between the "<" button and Done, clear of the page text.
        graphics.centeredText(this.font, Component.literal((this.spread + 1) + " / " + this.spreads.size()), x + 20 + (SPREAD_WIDTH / 2 - 60) / 2,
                y + PAGE_HEIGHT + 10, 0xFFE0E0E0);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    // ---- Field Notes ----

    private void drawNotes(GuiGraphicsExtractor graphics, int left, int right, int y) {

        int centre = left + PAGE_WIDTH / 2;
        graphics.centeredText(this.font, Component.translatable("journal.paranormalcraft.field_notes.title"), centre, y + 8, INK);
        // Left page: the chain, top to bottom.
        int[] rows = {y + 26, y + 70, y + 114, y + 158};
        List<List<JournalTypes.Icon>> chain = List.of(
                List.of(JournalTypes.Icon.item(ModItems.EMF_READER), JournalTypes.Icon.item(ModItems.SPIRIT_THERMOMETER),
                        JournalTypes.Icon.item(ModItems.SPECTRAL_GOGGLES)),
                List.of(JournalTypes.Icon.item(ModItems.TETHER_WAND)),
                List.of(JournalTypes.Icon.item(ModItems.GHOST_TRAP)),
                List.of(JournalTypes.Icon.item(ModItems.CONTAINMENT_UNIT)));
        String[] labels = {"find", "weaken", "catch", "contain"};
        for (int i = 0; i < rows.length; i++) {
            List<JournalTypes.Icon> icons = chain.get(i);
            int width = icons.size() * 20 - 4;
            for (int n = 0; n < icons.size(); n++) {
                icon(graphics, icons.get(n), centre - width / 2 + n * 20, rows[i]);
            }
            graphics.centeredText(this.font, Component.translatable("journal.paranormalcraft.diagram." + labels[i]), centre, rows[i] + 19, INK);
            if (i < rows.length - 1) {
                arrowDown(graphics, centre, rows[i] + 30, rows[i + 1] - 3);
            }
        }
        // From Contain, across the spine, to a bus on the right page that branches to the three choices.
        int busX = right + 22;
        int fromY = rows[3] + 8;
        graphics.fill(centre + 12, fromY, busX + 1, fromY + 1, LINE);
        int[] branches = {y + 26, y + 84, y + 142};
        graphics.fill(busX, branches[0] + 8, busX + 1, fromY + 1, LINE);
        graphics.centeredText(this.font, Component.translatable("journal.paranormalcraft.diagram.choose"), right + PAGE_WIDTH / 2, y + 8, INK);
        List<JournalTypes.Icon> outcomes = List.of(JournalTypes.Icon.item(ModItems.LINGERING_ECTOPLASM),
                JournalTypes.Icon.effect(() -> RegistrationHelper.holder(ModEffects.RESTFUL)),
                JournalTypes.Icon.stack(JournalScreen::boundVessel, Component.translatable("item.paranormalcraft.bound_spirit_vessel")));
        String[] outcomeLabels = {"process", "release", "bind"};
        int iconX = right + 70;
        for (int i = 0; i < branches.length; i++) {
            arrowRight(graphics, busX, iconX - 4, branches[i] + 8);
            icon(graphics, outcomes.get(i), iconX, branches[i]);
            graphics.centeredText(this.font, Component.translatable("journal.paranormalcraft.diagram." + outcomeLabels[i]), iconX + 8,
                    branches[i] + 19, INK);
            graphics.text(this.font, Component.translatable("journal.paranormalcraft.diagram." + outcomeLabels[i] + ".gives"), iconX + 24,
                    branches[i] + 4, FADED, false);
        }
    }

    private void arrowDown(GuiGraphicsExtractor graphics, int x, int y0, int y1) {

        graphics.fill(x, y0, x + 1, y1, LINE);
        for (int i = 0; i < 3; i++) {
            graphics.fill(x - i, y1 - 1 - i, x + 1 + i, y1 - i, LINE);
        }
    }

    private void arrowRight(GuiGraphicsExtractor graphics, int x0, int x1, int y) {

        graphics.fill(x0, y, x1, y + 1, LINE);
        for (int i = 0; i < 3; i++) {
            graphics.fill(x1 - 1 - i, y - i, x1 - i, y + 1 + i, LINE);
        }
    }

    // ---- a ghost type ----

    private void drawType(GuiGraphicsExtractor graphics, TypePage page, int left, int right, int y) {

        Identifier id = Identifier.parse(page.type());
        String key = "journal.paranormalcraft." + id.getPath();
        graphics.centeredText(this.font, Component.translatable("entity." + id.getNamespace() + "." + id.getPath()), left + PAGE_WIDTH / 2, y + 8, INK);
        JournalTypes.TypeInfo info = JournalTypes.of(page.type());
        // Portrait.
        int boxLeft = left + (PAGE_WIDTH - 96) / 2;
        int boxTop = y + 20;
        graphics.fill(boxLeft, boxTop, boxLeft + 96, boxTop + 80, PORTRAIT_BACK);
        GhostEntity ghost = info == null ? null : portrait(page.type(), info);
        if (ghost != null) {
            ghost.setPortrait(page.stage() >= 2 ? GhostEntity.PORTRAIT_SEEN : GhostEntity.PORTRAIT_SILHOUETTE);
            float turn = (Util.getMillis() % TURN_MILLIS) / (float) TURN_MILLIS * 360.0F;
            drawPortrait(graphics, ghost, boxLeft, boxTop, boxLeft + 96, boxTop + 80, 46, turn);
        }
        if (page.stage() < 2) {
            graphics.centeredText(this.font, Component.translatable("journal.paranormalcraft.unseen"), left + PAGE_WIDTH / 2, boxTop + 70, FADED);
        }
        // Facts rows.
        if (info != null) {
            int rowY = y + 106;
            for (JournalTypes.Fact fact : info.facts()) {
                boolean unlocked = page.stage() >= fact.unlockStage();
                graphics.text(this.font, Component.translatable("journal.paranormalcraft.row." + fact.key()), left + MARGIN, rowY + 4,
                        unlocked ? INK : FADED, false);
                int iconX = left + 74;
                if (unlocked) {
                    for (JournalTypes.Icon icon : fact.icons()) {
                        icon(graphics, icon, iconX, rowY);
                        iconX += 18;
                    }
                    if (fact.note() != null) {
                        graphics.text(this.font, fact.note(), iconX + 2, rowY + 4, INK, false);
                    }
                } else {
                    unknown(graphics, iconX, rowY);
                }
                rowY += FACT_ROW;
            }
        }
        // Right page: the stage notes.
        int textY = y + 10;
        int textWidth = PAGE_WIDTH - 2 * MARGIN;
        for (int n = 1; n <= 4; n++) {
            if (n <= page.stage()) {
                Component text = Component.translatable(key + ".stage" + n);
                graphics.textWithWordWrap(this.font, text, right + MARGIN, textY, textWidth, INK, false);
                textY += this.font.wordWrapHeight(text, textWidth) + 7;
            } else {
                unknown(graphics, right + MARGIN, textY - 4);
                textY += 19;
            }
        }
        if (page.bound()) {
            Component text = Component.translatable(key + ".bound");
            graphics.fill(right + MARGIN, textY - 4, right + PAGE_WIDTH - MARGIN, textY - 3, LINE);
            graphics.textWithWordWrap(this.font, text, right + MARGIN, textY, textWidth, INK, false);
        }
    }

    private @Nullable GhostEntity portrait(String type, JournalTypes.TypeInfo info) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return this.portraits.computeIfAbsent(type, key -> {
            GhostEntity ghost = info.entity().get().create(minecraft.level, EntitySpawnReason.LOAD);
            if (ghost != null) {
                ghost.setPortrait(GhostEntity.PORTRAIT_SILHOUETTE);
                // Never added to the level, so it has no id; rendering asks for one. Negative ids can't clash.
                ghost.setId(PORTRAIT_ID_BASE - this.portraits.size());
            }
            return ghost;
        });
    }

    /**
     * Draws {@code ghost} turned {@code turn} degrees, the way the inventory screen draws the player.
     */
    private static void drawPortrait(GuiGraphicsExtractor graphics, GhostEntity ghost, int x0, int y0, int x1, int y1, int size, float turn) {

        EntityRenderer<? super GhostEntity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(ghost);
        EntityRenderState state = renderer.createRenderState(ghost, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0F + turn;
            living.yRot = 0.0F;
            living.xRot = 0.0F;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1.0F;
        }
        Quaternionf tilt = new Quaternionf().rotateX((float) Math.toRadians(-8.0));
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
        graphics.entity(state, size, translation, rotation, tilt, x0, y0, x1, y1);
    }

    // ---- captures ----

    private void drawCaptures(GuiGraphicsExtractor graphics, Captures captures, int left, int right, int y) {

        graphics.centeredText(this.font, Component.translatable("journal.paranormalcraft.captures.title"), left + PAGE_WIDTH / 2, y + 8, INK);
        if (captures.left().isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("journal.paranormalcraft.captures.empty"), left + PAGE_WIDTH / 2, y + 40, FADED);
            return;
        }
        drawCaptureRows(graphics, captures.left(), left, y + 24);
        drawCaptureRows(graphics, captures.right(), right, y + 24);
    }

    private void drawCaptureRows(GuiGraphicsExtractor graphics, List<CaptureLog.Entry> entries, int pageX, int startY) {

        int rowY = startY;
        for (CaptureLog.Entry entry : entries) {
            Identifier type = Identifier.parse(entry.type());
            JournalTypes.TypeInfo info = JournalTypes.of(entry.type());
            Component typeName = Component.translatable("entity." + type.getNamespace() + "." + type.getPath());
            if (info != null) {
                icon(graphics, JournalTypes.Icon.stack(() -> new ItemStack(info.logIcon().get()), typeName), pageX + MARGIN - 2, rowY);
            }
            String day = Component.translatable("journal.paranormalcraft.captures.day", entry.day()).getString();
            int dayWidth = this.font.width(day);
            int outcomeX = pageX + PAGE_WIDTH - MARGIN - 16;
            int nameWidth = outcomeX - 6 - dayWidth - (pageX + MARGIN + 18);
            graphics.text(this.font, this.font.plainSubstrByWidth(entry.name(), nameWidth), pageX + MARGIN + 18, rowY + 4, INK, false);
            graphics.text(this.font, day, outcomeX - 4 - dayWidth, rowY + 4, FADED, false);
            icon(graphics, outcomeIcon(entry.outcome()), outcomeX, rowY);
            graphics.fill(pageX + MARGIN, rowY + CAPTURE_ROW - 2, pageX + PAGE_WIDTH - MARGIN, rowY + CAPTURE_ROW - 1, 0x33000000);
            rowY += CAPTURE_ROW;
        }
    }

    private static JournalTypes.Icon outcomeIcon(String outcome) {

        Component name = Component.translatable("journal.paranormalcraft.outcome." + outcome);
        Supplier<ItemStack> stack = switch (outcome) {
            case "processed" -> () -> new ItemStack(ModItems.LINGERING_ECTOPLASM.get());
            case "bound" -> JournalScreen::boundVessel;
            case "released" -> null;
            default -> () -> new ItemStack(ModItems.FILLED_GHOST_TRAP.get());
        };
        if (stack == null) {
            return new JournalTypes.Icon(null, () -> RegistrationHelper.holder(ModEffects.RESTFUL), name);
        }
        return JournalTypes.Icon.stack(stack, name);
    }

    private static ItemStack boundVessel() {

        ItemStack stack = new ItemStack(ModItems.SPIRIT_VESSEL.get());
        stack.set(ModDataComponents.CAPTURED_GHOST.get(), new CapturedGhost("", new CompoundTag(), "", 0L, "", "", Optional.empty()));
        return stack;
    }

    // ---- icons ----

    private void icon(GuiGraphicsExtractor graphics, JournalTypes.Icon icon, int x, int y) {

        if (icon.effect() != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(icon.effect().get()), x - 1, y - 1, 18, 18);
        } else {
            graphics.item(icon.stack(), x, y);
        }
        hover(graphics, x, y, icon.name());
    }

    /** The "?" for a locked row or an unreached stage. */
    private void unknown(GuiGraphicsExtractor graphics, int x, int y) {

        graphics.fill(x, y, x + 16, y + 16, 0x22000000);
        graphics.fill(x, y, x + 16, y + 1, LINE);
        graphics.fill(x, y + 15, x + 16, y + 16, LINE);
        graphics.fill(x, y, x + 1, y + 16, LINE);
        graphics.fill(x + 15, y, x + 16, y + 16, LINE);
        graphics.centeredText(this.font, Component.literal("?"), x + 8, y + 4, FADED);
        hover(graphics, x, y, Component.translatable("journal.paranormalcraft.unknown"));
    }

    private void hover(GuiGraphicsExtractor graphics, int x, int y, Component name) {

        if (this.mouseX >= x && this.mouseX < x + 16 && this.mouseY >= y && this.mouseY < y + 16) {
            graphics.setTooltipForNextFrame(name, this.mouseX, this.mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {

        return false;
    }
}
