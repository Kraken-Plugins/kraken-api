package unit.com.kraken.api.input;

import com.kraken.api.input.mouse.MouseRecorder;
import net.runelite.client.input.MouseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Canvas;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MouseRecorderTest {

    private final Canvas canvas = new Canvas();

    private MouseEvent moved(int x, int y) {
        return new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false);
    }

    private MouseEvent pressed(int x, int y) {
        return new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1);
    }

    @Test
    void labelsCannotEscapeDataDirectory(@TempDir Path dir) {
        MouseRecorder recorder = new MouseRecorder(mock(MouseManager.class), dir);
        Path escaped = recorder.fileFor("../../evil");
        assertEquals(dir.toAbsolutePath().normalize(), escaped.getParent());
        assertEquals(".._.._evil.json", escaped.getFileName().toString());
        assertEquals("my_label.json", recorder.fileFor("my label").getFileName().toString());
        assertEquals("default.json", recorder.fileFor("..").getFileName().toString());
    }

    @Test
    void stopDrainsWritesAndLaterRecordingsGoToTheirOwnFile(@TempDir Path dir) throws Exception {
        MouseRecorder recorder = new MouseRecorder(mock(MouseManager.class), dir);

        recorder.start("first");
        recorder.mouseMoved(moved(1, 1));
        recorder.mouseMoved(moved(2, 2));
        recorder.mousePressed(pressed(3, 3));
        recorder.stop();

        recorder.start("second");
        recorder.mouseMoved(moved(5, 5));
        recorder.mousePressed(pressed(6, 6));
        recorder.stop();

        List<String> first = Files.readAllLines(dir.resolve("first.json"));
        List<String> second = Files.readAllLines(dir.resolve("second.json"));
        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertTrue(first.get(0).contains("\"first\""));
        assertFalse(first.get(0).contains("\"second\""));
        assertTrue(second.get(0).contains("\"second\""));
    }

    @Test
    void oversizedGestureIsDiscardedAndRestarted(@TempDir Path dir) throws Exception {
        MouseRecorder recorder = new MouseRecorder(mock(MouseManager.class), dir);
        recorder.start("cap");
        for (int i = 0; i < MouseRecorder.MAX_POINTS_PER_GESTURE + 10; i++) {
            recorder.mouseMoved(moved(i, 0));
        }
        recorder.mousePressed(pressed(0, 0));
        recorder.stop();

        String gesture = Files.readAllLines(dir.resolve("cap.json")).get(0);
        assertTrue(gesture.contains("\"startX\":" + MouseRecorder.MAX_POINTS_PER_GESTURE), gesture.substring(0, 120));
    }
}
