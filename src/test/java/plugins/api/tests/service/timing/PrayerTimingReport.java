package plugins.api.tests.service.timing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A prayer timing run written out for a human to look at: an HTML page that charts where in each tick every click
 * went out, and a CSV of the same events for a spreadsheet or a notebook.
 *
 * <p>The page is {@value #TEMPLATE} with the report's JSON in place of {@value #PLACEHOLDER}; it needs no network and
 * opens in any browser. DevTools in kraken-plugins keeps an identical copy of the template and writes the same JSON,
 * so both tools' reports read the same way.
 *
 * <p>CSV rows, one per event, sorted by tick then offset: {@code tick} rows carry the prayer's state at the start of
 * the tick in {@code direction} ({@code up} or {@code down}), {@code hit} in {@code kind} on hit ticks and any verdict in
 * {@code detail}; {@code click} rows carry the click's direction, kind and asked offset; every other row is an event
 * such as a varp change, with its label in {@code detail}.
 */
@Value
@Builder(toBuilder = true)
public class PrayerTimingReport {

    /**
     * The template on the classpath.
     */
    public static final String TEMPLATE = "/plugins/api/tests/service/timing/prayer-timing-report.html";

    /**
     * The template's stand-in for the report's JSON.
     */
    public static final String PLACEHOLDER = "__REPORT_JSON__";

    /**
     * The CSV header.
     */
    public static final String CSV_HEADER = "tick,ms,event,direction,kind,requested_ms,detail";

    private static final Gson GSON = new GsonBuilder().create();

    String title;
    String subtitle;
    String prayer;

    @Builder.Default
    int tickMs = 600;

    int cutoffMs;

    @Singular
    List<Check> checks;

    @Singular
    List<String> notes;

    @Singular
    List<Tick> ticks;

    @Singular
    List<Click> clicks;

    @Singular
    List<Marker> markers;

    /**
     * @return the report as JSON. Gson escapes {@code <} and {@code >}, so it can sit inside a script element.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * @param template the HTML template
     * @return the page, the template with this report's JSON in place of {@value #PLACEHOLDER}
     */
    public String toHtml(String template) {
        if (!template.contains(PLACEHOLDER)) {
            throw new IllegalArgumentException("template has no " + PLACEHOLDER);
        }
        return template.replace(PLACEHOLDER, toJson());
    }

    /**
     * @return every event as a CSV row under {@link #CSV_HEADER}, sorted by tick then offset
     */
    public String toCsv() {
        List<Row> rows = new ArrayList<>();
        for (Tick tick : ticks) {
            rows.add(new Row(tick.getTick(), 0, 0, "tick", tick.isUp() ? "up" : "down", tick.isHit() ? "hit" : "", "",
                    tick.getVerdict()));
        }
        for (Click click : clicks) {
            rows.add(new Row(click.getTick(), click.getSentMs(), 1, "click", click.isOn() ? "on" : "off",
                    click.getKind(), click.getRequestedMs() >= 0 ? String.valueOf(click.getRequestedMs()) : "", ""));
        }
        for (Marker marker : markers) {
            rows.add(new Row(marker.getTick(), marker.getMs(), 1, marker.getType(), "", "", "", marker.getLabel()));
        }
        rows.sort(Comparator.comparingInt(Row::getTick).thenComparingInt(Row::getOrder).thenComparingInt(Row::getMs));

        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (Row row : rows) {
            csv.append(row.getTick()).append(',').append(row.getMs()).append(',').append(cell(row.getEvent()))
                    .append(',').append(cell(row.getDirection())).append(',').append(cell(row.getKind()))
                    .append(',').append(cell(row.getRequested())).append(',').append(cell(row.getDetail()))
                    .append('\n');
        }
        return csv.toString();
    }

    /**
     * Writes {@code baseName.html} and {@code baseName.csv} into a directory, creating it when needed.
     *
     * @param directory where to write
     * @param baseName  the file name without its extension
     * @return the HTML page's path; the CSV sits beside it
     * @throws IOException when the template cannot be read or a file cannot be written
     */
    public Path write(Path directory, String baseName) throws IOException {
        Files.createDirectories(directory);
        Path html = directory.resolve(baseName + ".html");
        Files.writeString(html, toHtml(template()), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve(baseName + ".csv"), toCsv(), StandardCharsets.UTF_8);
        return html;
    }

    /**
     * @return the HTML template from the classpath
     * @throws IOException when it is missing or cannot be read
     */
    public static String template() throws IOException {
        try (InputStream in = PrayerTimingReport.class.getResourceAsStream(TEMPLATE)) {
            if (in == null) {
                throw new IOException("missing report template " + TEMPLATE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Quotes a CSV cell when it holds a comma, a quote or a line break.
     */
    private static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * One check and its outcome.
     */
    @Value
    public static class Check {
        String name;
        boolean passed;
        String detail;
    }

    /**
     * One game tick.
     */
    @Value
    public static class Tick {
        int tick;

        /**
         * True when the prayer must be up on this tick.
         */
        boolean hit;

        /**
         * Whether the prayer read as active at the start of the tick.
         */
        boolean up;

        /**
         * What went wrong or was allowed on this tick, such as {@code missed}, {@code stray}, {@code early} or
         * {@code held}; empty when nothing is worth noting.
         */
        String verdict;
    }

    /**
     * One click on the prayer.
     */
    @Value
    public static class Click {
        /**
         * The tick the click went out in.
         */
        int tick;

        boolean on;

        /**
         * Milliseconds after the tick started at which the click went out.
         */
        int sentMs;

        /**
         * The offset the click asked for, or -1 when it asked for none.
         */
        int requestedMs;

        /**
         * What the click tested, such as {@code SWEEP}; empty when it had no kind.
         */
        String kind;
    }

    /**
     * Anything else that happened at a point in a tick: a varp change, a sound, a cue, a plan.
     */
    @Value
    public static class Marker {
        int tick;
        int ms;

        /**
         * One of {@code plan}, {@code appear}, {@code vanish}, {@code feint}, {@code cancel}, {@code varp} and
         * {@code sound}, which the page draws with their own shapes.
         */
        String type;

        /**
         * Shown in the tooltip and the table; may be empty.
         */
        String label;
    }

    @Value
    private static class Row {
        int tick;
        int ms;
        int order;
        String event;
        String direction;
        String kind;
        String requested;
        String detail;
    }
}
