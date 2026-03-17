# Utilities

This document describes helper utilities available in the `com.kraken.api.util` and `com.kraken.api.overlay` packages, 
with guidance on how to use them inside RuneLite plugins.

## Usage Patterns In RuneLite Plugins

Most overlay utilities are standard RuneLite overlay components. Typical usage patterns:

1. `@Inject` the utility or overlay into your plugin.
2. Add overlays to the `OverlayManager` in `startUp()`.
3. Remove overlays in `shutDown()`.
4. Keep all overlay rendering in `Overlay#render(Graphics2D)`.

Examples in this document assume standard RuneLite plugin setup and DI configuration.

## `com.kraken.api.util`

### `MathUtils`

Math helpers are used across the API.

- `modInverse(BigInteger val, int bits)`
  - Computes `val` mod inverse under a left-shifted modulus (`2^bits`). If no inverse exists, it returns the original `val`.
- `modInverse(long val)`
  - Convenience overload using 64-bit shift (`2^64`).
- `clamp(int value, int minInclusive, int maxInclusive)`
  - Clamps a value into an inclusive range.
- `chebyshevDistance(int ax, int ay, int bx, int by)`
  - Returns Chebyshev distance between tiles. Useful for grid-based movement logic.
- `overlaps(int aX, int aY, int aSize, int bX, int bY, int bSize)`
  - Tests overlap between two square tile footprints using their SW corners and sizes.

Example usage:

```java
import com.kraken.api.util.MathUtils;

int capped = MathUtils.clamp(level, 1, 99);
int distance = MathUtils.chebyshevDistance(a.getX(), a.getY(), b.getX(), b.getY());
boolean overlapping = MathUtils.overlaps(aX, aY, aSize, bX, bY, bSize);
```

### `RandomUtils`

Random helpers for deterministic-ish gameplay utilities.

- `randomIntBetween(int min, int max)`
  - Uniform random integer between min and max (inclusive).
- `randomFromSet(Set<Integer> set)`
  - Returns a random element from a non-empty integer set.
- `randomDelay()`
  - Returns a pseudo-random delay with a normal distribution, clamped between 1 and 13000.

Example usage:

```java
import com.kraken.api.util.RandomUtils;

int roll = RandomUtils.randomIntBetween(1, 100);
long delayMs = RandomUtils.randomDelay();
```

### `StringUtils`

String utilities for RuneLite-specific formatting and safe text handling.

- `getIndex(String[] terms, String term)`
  - Case-insensitive index lookup in string arrays.
- `stripColTags(String source)`
  - Removes `<col=...>` tags from a single string.
- `stripColTags(String[] sourceList)`
  - Removes `<col=...>` tags from all strings in an array.
- `addColTags(String text)`
  - Wraps text with the default Kraken API color tag (`<col=ff9040>`). Returns the original text if null or empty.
- `encrypt(String plaintext, String key)`
  - AES/CBC/PKCS5Padding encrypt; prepends a random IV and returns Base64. The key is expected to be a Base64-encoded 32-byte key.
- `decrypt(String base64IvAndCiphertext, String key)`
  - Decrypts output produced by `encrypt` using the same key.

Example usage:

```java
import com.kraken.api.util.StringUtils;

String colored = StringUtils.addColTags("Hello");
String clean = StringUtils.stripColTags("<col=ff0000>Danger</col>");
```

Encryption example:

```java
import com.kraken.api.util.StringUtils;

String encrypted = StringUtils.encrypt(secretText, base64Key);
String decrypted = StringUtils.decrypt(encrypted, base64Key);
```

## `com.kraken.api.overlay`

### `MouseOverlay`

A debug overlay that can render a mouse crosshair and an optional trail on the game canvas.

- `setRenderCrosshair(boolean)` toggles crosshair rendering.
- `setRenderTrail(boolean)` toggles trail rendering.

Integration example:

```java
import com.google.inject.Inject;
import com.kraken.api.overlay.MouseOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

public class MyPlugin extends Plugin {
    @Inject private OverlayManager overlayManager;
    @Inject private MouseOverlay mouseOverlay;

    @Override
    protected void startUp() {
        mouseOverlay.setRenderCrosshair(true);
        mouseOverlay.setRenderTrail(true);
        overlayManager.add(mouseOverlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(mouseOverlay);
    }
}
```

### Table Overlay Utilities

Table components allow rendering a compact, multi-column table inside standard RuneLite overlays.

- `TableComponent`
  - Main renderer. Holds columns and rows and renders them into a `Graphics2D` context.
- `TableRow`
  - A row definition with optional row color and alignment.
- `TableElement`
  - A cell definition with optional alignment, color, and string content.
- `TableAlignment`
  - Alignment enum: `LEFT`, `CENTER`, `RIGHT`.

Typical flow:

1. Build columns with `TableElement` or simple `String` headers.
2. Add rows with `addRow(String...)` or `addRows(TableRow...)`.
3. Render the table in your overlay’s `render()` method.

Example usage inside an overlay:

```java
import com.kraken.api.overlay.table.TableAlignment;
import com.kraken.api.overlay.table.TableComponent;
import com.kraken.api.overlay.table.TableElement;
import com.kraken.api.overlay.table.TableRow;
import net.runelite.client.ui.overlay.Overlay;
import java.util.List;

public class StatsOverlay extends Overlay {
    private final TableComponent table = new TableComponent();

    public StatsOverlay() {
        table.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
        table.setColumns(
            TableElement.builder().content("Stat").build(),
            TableElement.builder().content("Value").build()
        );
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        table.setRows(
            TableRow.builder()
                .elements(List.of(
                    TableElement.builder().content("Kills").build(),
                    TableElement.builder().content("42").build()
                ))
                .build()
        );

        return table.render(graphics);
    }
}
```

Notes:

- `TableComponent` automatically wraps text and sizes columns based on content.
- Cell alignment/color overrides row alignment/color; row overrides column; column overrides default.

### Log Overlay Utilities

Log overlay components provide a lightweight on-screen log viewer for your plugin. It captures Logback logs from your package and renders them inside a `PanelComponent`.

Components:

- `PluginLogger`
  - Attaches to a Logback logger for a package and buffers recent log entries.
- `OverlayAppender`
  - Internal appender that forwards Logback events to `PluginLogger`.
- `LogOverlayComponent`
  - Renders the buffered log entries into a `PanelComponent`.
- `LogEntry` and `LogLevel`
  - Model for log lines and severity.

Integration example:

```java
import com.google.inject.Inject;
import com.kraken.api.overlay.log.LogOverlayComponent;
import com.kraken.api.overlay.log.PluginLogger;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class LoggingOverlay extends Overlay {
    private final PanelComponent panel = new PanelComponent();

    @Inject private PluginLogger pluginLogger;
    @Inject private LogOverlayComponent logOverlayComponent;

    @Override
    public Dimension render(Graphics2D graphics) {
        panel.getChildren().clear();
        // ... add your normal overlay lines here ...
        logOverlayComponent.addTo(panel);
        return panel.render(graphics);
    }
}

public class MyPlugin extends Plugin {
    @Inject private PluginLogger pluginLogger;

    @Override
    protected void startUp() {
        pluginLogger.attach("com.example.myplugin");
    }

    @Override
    protected void shutDown() {
        pluginLogger.detach();
    }
}
```

Notes:

- `PluginLogger.attach(...)` should be called during `startUp()` and `detach()` during `shutDown()`.
- Log output includes a timestamp, abbreviated logger name, thread, and message.
- The default buffer size is 6 entries; construct `PluginLogger(int maxEntries)` if you need a larger buffer.
