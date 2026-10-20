package io.github.mdw22.paranormalcraft.playtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A playtest run's results and their Markdown form, as plain logic so the format is unit tested. The file has a
 * header (checklist, date, build, player, totals) and one table row per step: result, auto-check, time spent and notes.
 */
public final class PlaytestResults {

    public enum Status {
        PENDING("not reached"),
        PASS("pass"),
        FAIL("**FAIL**"),
        SKIP("skip");

        private final String label;

        Status(String label) {

            this.label = label;
        }

        public String label() {

            return this.label;
        }
    }

    public enum AutoCheck {
        NONE("—"),
        WAITING("not met"),
        PASSED("✓ passed");

        private final String label;

        AutoCheck(String label) {

            this.label = label;
        }

        public String label() {

            return this.label;
        }
    }

    /** One step's outcome; {@code millis} is the real time spent on it, summed over visits. */
    public static final class StepResult {

        private final String id;
        private final String title;
        private Status status = Status.PENDING;
        private AutoCheck autoCheck;
        private final List<String> notes = new ArrayList<>();
        private long millis;

        public StepResult(String id, String title, boolean hasAutoCheck) {

            this.id = id;
            this.title = title;
            this.autoCheck = hasAutoCheck ? AutoCheck.WAITING : AutoCheck.NONE;
        }

        public String id() {

            return this.id;
        }

        public String title() {

            return this.title;
        }

        public Status status() {

            return this.status;
        }

        public void setStatus(Status status) {

            this.status = status;
        }

        public AutoCheck autoCheck() {

            return this.autoCheck;
        }

        public void autoCheckPassed() {

            this.autoCheck = AutoCheck.PASSED;
        }

        public List<String> notes() {

            return List.copyOf(this.notes);
        }

        public void addNote(String note) {

            if (!note.isBlank()) {
                this.notes.add(note.strip());
            }
        }

        public long millis() {

            return this.millis;
        }

        public void addMillis(long millis) {

            this.millis += Math.max(0, millis);
        }
    }

    /**
     * The run's header facts.
     *
     * @param ending "finished", or why it stopped early
     */
    public record Header(String checklist, String title, String date, String build, String player, String ending) {
    }

    private PlaytestResults() {
    }

    /** "m:ss" for a duration. */
    public static String duration(long millis) {

        long seconds = Math.max(0, millis) / 1000;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /** Totals: "7 pass, 1 fail, 1 skip, 0 not reached (9 steps)". */
    public static String totals(List<StepResult> steps) {

        int[] counts = new int[Status.values().length];
        for (StepResult step : steps) {
            counts[step.status().ordinal()]++;
        }
        return String.format(Locale.ROOT, "%d pass, %d fail, %d skip, %d not reached (%d steps)", counts[Status.PASS.ordinal()],
                counts[Status.FAIL.ordinal()], counts[Status.SKIP.ordinal()], counts[Status.PENDING.ordinal()], steps.size());
    }

    /**
     * The results as Markdown.
     */
    public static String markdown(Header header, List<StepResult> steps) {

        StringBuilder out = new StringBuilder();
        out.append("# Playtest: ").append(header.checklist()).append(" — ").append(header.title()).append("\n\n");
        out.append("- **Date:** ").append(header.date()).append('\n');
        out.append("- **Build:** ").append(header.build()).append('\n');
        out.append("- **Player:** ").append(header.player()).append('\n');
        out.append("- **Run:** ").append(header.ending()).append('\n');
        out.append("- **Result:** ").append(totals(steps)).append("\n\n");
        out.append("| # | Step | Result | Auto-check | Time | Notes |\n");
        out.append("| --- | --- | --- | --- | --- | --- |\n");
        for (int i = 0; i < steps.size(); i++) {
            StepResult step = steps.get(i);
            out.append("| ").append(i + 1)
                    .append(" | ").append(cell(step.title())).append(" (`").append(step.id()).append("`)")
                    .append(" | ").append(step.status().label())
                    .append(" | ").append(step.autoCheck().label())
                    .append(" | ").append(duration(step.millis()))
                    .append(" | ").append(step.notes().isEmpty() ? "" : cell(String.join(" / ", step.notes())))
                    .append(" |\n");
        }
        return out.toString();
    }

    /** Text safe inside a table cell: no pipes or line breaks. */
    static String cell(String text) {

        return text.replace("|", "\\|").replace("\r", " ").replace("\n", " ").strip();
    }
}
