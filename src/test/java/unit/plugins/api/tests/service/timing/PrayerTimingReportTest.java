package unit.plugins.api.tests.service.timing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import plugins.api.tests.service.timing.PrayerTimingReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrayerTimingReportTest {

    private static PrayerTimingReport report() {
        return PrayerTimingReport.builder()
                .title("Timed Prayer Flicks")
                .subtitle("sample")
                .prayer("Thick Skin")
                .cutoffMs(300)
                .check(new PrayerTimingReport.Check("Prayer up on every hit tick", true, "up on every hit"))
                .note("Lateness of swept clicks: avg 9.0 ms")
                .tick(new PrayerTimingReport.Tick(101, true, true, ""))
                .tick(new PrayerTimingReport.Tick(100, false, false, ""))
                .click(new PrayerTimingReport.Click(100, true, 135, 130, "SWEEP"))
                .marker(new PrayerTimingReport.Marker(100, 150, "varp", "now reads up, </script> in a label"))
                .marker(new PrayerTimingReport.Marker(101, 40, "sound", "deactivation, with a comma"))
                .build();
    }

    /**
     * @return the JSON inside the page's report-data element
     */
    private static JsonObject embeddedJson(String html) {
        String open = "<script id=\"report-data\" type=\"application/json\">";
        int start = html.indexOf(open) + open.length();
        int end = html.indexOf("</script>", start);
        return new Gson().fromJson(html.substring(start, end), JsonObject.class);
    }

    @Test
    void thePageCarriesTheReportInPlaceOfThePlaceholder() throws IOException {
        String html = report().toHtml(PrayerTimingReport.template());

        assertFalse(html.contains(PrayerTimingReport.PLACEHOLDER));
        JsonObject json = embeddedJson(html);
        assertEquals("Timed Prayer Flicks", json.get("title").getAsString());
        assertEquals(300, json.get("cutoffMs").getAsInt());
        assertEquals(600, json.get("tickMs").getAsInt());
        assertEquals(135, json.getAsJsonArray("clicks").get(0).getAsJsonObject().get("sentMs").getAsInt());
        assertEquals(130, json.getAsJsonArray("clicks").get(0).getAsJsonObject().get("requestedMs").getAsInt());
    }

    @Test
    void aLabelCannotCloseTheDataElement() throws IOException {
        String html = report().toHtml(PrayerTimingReport.template());

        assertEquals("now reads up, </script> in a label",
                embeddedJson(html).getAsJsonArray("markers").get(0).getAsJsonObject().get("label").getAsString());
    }

    @Test
    void aTemplateWithoutThePlaceholderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> report().toHtml("<html></html>"));
    }

    @Test
    void theCsvListsEveryEventByTickThenOffset() {
        List<String> lines = report().toCsv().lines().toList();

        assertEquals(PrayerTimingReport.CSV_HEADER, lines.get(0));
        assertEquals("100,0,tick,down,,,", lines.get(1));
        assertEquals("100,135,click,on,SWEEP,130,", lines.get(2));
        assertEquals("100,150,varp,,,,\"now reads up, </script> in a label\"", lines.get(3));
        assertEquals("101,0,tick,up,hit,,", lines.get(4));
        assertEquals("101,40,sound,,,,\"deactivation, with a comma\"", lines.get(5));
        assertEquals(6, lines.size());
    }

    @Test
    void writingPutsThePageAndItsDataSideBySide(@TempDir Path directory) throws IOException {
        Path html = report().write(directory.resolve("prayer-timing"), "run");

        assertEquals(directory.resolve("prayer-timing").resolve("run.html"), html);
        assertTrue(Files.readString(html, StandardCharsets.UTF_8).contains("\"title\":\"Timed Prayer Flicks\""));
        assertEquals(report().toCsv(),
                Files.readString(directory.resolve("prayer-timing").resolve("run.csv"), StandardCharsets.UTF_8));
    }
}
