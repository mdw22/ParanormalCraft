package io.github.mdw22.paranormalcraft.client.journal;

import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.network.JournalPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Field Journal, read-only and book-like (placeholder art, drawn with fills). Pages: "Field notes", then two pages
 * per ghost type the player has met (stages 1–2 and 3–4; stages not reached yet show a placeholder line), then the
 * capture log (ten entries a page).
 * All text is lang ({@code journal.paranormalcraft.*}).
 */
public class JournalScreen extends Screen {

    private static final int PAGE_WIDTH = 224;
    private static final int PAGE_HEIGHT = 178;
    private static final int MARGIN = 14;
    private static final int PAPER = 0xFFE9DFC4;
    private static final int PAPER_EDGE = 0xFF8C7A55;
    private static final int INK = 0xFF2E2618;
    private static final int FADED = 0xFF7A6A4C;
    private static final int CAPTURES_PER_PAGE = 8;

    /** A page: a title and its paragraphs. */
    private record Page(Component title, List<Component> paragraphs) {
    }

    private final List<Page> pages;
    private int page;
    private Button previous;
    private Button next;

    public JournalScreen(JournalPayload payload) {

        super(Component.translatable("item.paranormalcraft.field_journal"));
        this.pages = buildPages(payload);
    }

    /**
     * Client handler for {@link JournalPayload}: opens the journal.
     */
    public static void open(JournalPayload payload) {

        JournalScreen screen = new JournalScreen(payload);
        ClientTrace.event("journal_screen", line -> {
            line.addProperty("pages", screen.pages.size());
            payload.stages().forEach(stage -> line.addProperty(stage.type(), stage.stage()));
        });
        Minecraft.getInstance().gui.setScreen(screen);
    }

    private static List<Page> buildPages(JournalPayload payload) {

        List<Page> pages = new ArrayList<>();
        pages.add(new Page(Component.translatable("journal.paranormalcraft.field_notes.title"),
                List.of(Component.translatable("journal.paranormalcraft.field_notes.1"), Component.translatable("journal.paranormalcraft.field_notes.2"),
                        Component.translatable("journal.paranormalcraft.field_notes.3"))));
        for (JournalPayload.TypeStage stage : payload.stages()) {
            if (stage.stage() <= 0) {
                continue;
            }
            // Two pages per ghost type: stages 1-2, then 3-4.
            String key = "journal.paranormalcraft." + Identifier.parse(stage.type()).getPath();
            for (int first = 1; first <= 3; first += 2) {
                List<Component> paragraphs = new ArrayList<>();
                for (int n = first; n <= first + 1; n++) {
                    paragraphs.add(n <= stage.stage() ? Component.translatable(key + ".stage" + n)
                            : Component.translatable("journal.paranormalcraft.locked." + n));
                }
                pages.add(new Page(Component.translatable(key + ".title"), paragraphs));
            }
        }
        List<CaptureLog.Entry> captures = payload.captures();
        if (captures.isEmpty()) {
            pages.add(new Page(Component.translatable("journal.paranormalcraft.captures.title"),
                    List.of(Component.translatable("journal.paranormalcraft.captures.empty"))));
        }
        for (int start = 0; start < captures.size(); start += CAPTURES_PER_PAGE) {
            List<Component> lines = new ArrayList<>();
            for (CaptureLog.Entry entry : captures.subList(start, Math.min(captures.size(), start + CAPTURES_PER_PAGE))) {
                Identifier type = Identifier.parse(entry.type());
                MutableComponent line = Component.translatable("journal.paranormalcraft.captures.entry", entry.name(),
                        Component.translatable("entity." + type.getNamespace() + "." + type.getPath()), entry.day(),
                        Component.translatable("journal.paranormalcraft.outcome." + entry.outcome()));
                lines.add(line);
            }
            pages.add(new Page(Component.translatable("journal.paranormalcraft.captures.title"), lines));
        }
        return pages;
    }

    public int pageCount() {

        return this.pages.size();
    }

    public int page() {

        return this.page;
    }

    public void setPage(int page) {

        this.page = Math.clamp(page, 0, this.pages.size() - 1);
        updateButtons();
    }

    private int left() {

        return (this.width - PAGE_WIDTH) / 2;
    }

    private int top() {

        return (this.height - PAGE_HEIGHT) / 2;
    }

    @Override
    protected void init() {

        int buttonY = top() + PAGE_HEIGHT + 4;
        this.previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> setPage(this.page - 1))
                .bounds(left(), buttonY, 20, 20).build());
        this.next = addRenderableWidget(Button.builder(Component.literal(">"), button -> setPage(this.page + 1))
                .bounds(left() + PAGE_WIDTH - 20, buttonY, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left() + PAGE_WIDTH / 2 - 40, buttonY, 80, 20).build());
        updateButtons();
    }

    private void updateButtons() {

        if (this.previous != null) {
            this.previous.active = this.page > 0;
            this.next.active = this.page < this.pages.size() - 1;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {

        int x = left();
        int y = top();
        graphics.fill(x - 2, y - 2, x + PAGE_WIDTH + 2, y + PAGE_HEIGHT + 2, PAPER_EDGE);
        graphics.fill(x, y, x + PAGE_WIDTH, y + PAGE_HEIGHT, PAPER);
        Page current = this.pages.get(this.page);
        graphics.centeredText(this.font, current.title(), x + PAGE_WIDTH / 2, y + 10, INK);
        int textY = y + 26;
        int textWidth = PAGE_WIDTH - 2 * MARGIN;
        for (Component paragraph : current.paragraphs()) {
            boolean locked = paragraph.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                    && contents.getKey().startsWith("journal.paranormalcraft.locked.");
            graphics.textWithWordWrap(this.font, paragraph, x + MARGIN, textY, textWidth, locked ? FADED : INK, false);
            textY += this.font.wordWrapHeight(paragraph, textWidth) + 5;
        }
        graphics.centeredText(this.font, Component.literal((this.page + 1) + " / " + this.pages.size()), x + PAGE_WIDTH / 2,
                y + PAGE_HEIGHT - 12, FADED);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean isPauseScreen() {

        return false;
    }
}
