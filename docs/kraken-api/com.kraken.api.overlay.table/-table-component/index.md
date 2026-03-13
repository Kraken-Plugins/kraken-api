//[kraken-api](../../../index.md)/[com.kraken.api.overlay.table](../index.md)/[TableComponent](index.md)

# TableComponent

[Kraken API]\
open class [TableComponent](index.md)

The `TableComponent` class represents a customizable table component used for rendering tabular data in a graphical user interface. It provides options to define table properties, layout, column and row configurations, and controls for customization such as alignments, colors, and bounds. This class extends the capabilities of `LayoutableRenderableEntity`. 

The table supports the following features: 

- Dynamic addition and configuration of rows and columns.
- Per-element, per-row, and per-column customization of alignment and colors.
- Automatic adjustment of column widths based on content.
- Rendering in a graphical context with appropriate spacing and alignment.
- Helper utilities for string manipulation, text alignment, and graphical calculations.

The class is designed to be highly configurable and ensures that content is visually aligned and spaced across the table's structure. Note: This code was taken from the following source here: - https://github.com/lucid-plugins/SideloadPlugins/blob/master/src/main/java/com/lucidplugins/lucidgauntlet/table/TableComponent.java - https://github.com/OreoCupcakes/kotori-plugins/blob/master/kotoriutils/src/main/java/com/theplug/kotori/kotoriutils/rlapi/table/TableComponent.java All credit to the authors of this code goes to them. The projects are both open source, and the Kraken API provides these components as a utility to be used within other plugins. Kraken API in no way claims to have written or owns this code.

## Constructors

| | |
|---|---|
| [TableComponent](-table-component.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [addColumns](add-columns.md) | [Kraken API]<br>open fun [addColumns](add-columns.md)(columns: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[TableElement](../-table-element/index.md)&gt;) |
| [addRow](add-row.md) | [Kraken API]<br>open fun [addRow](add-row.md)(cells: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;) |
| [addRows](add-rows.md) | [Kraken API]<br>open fun [addRows](add-rows.md)(rows: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[TableRow](../-table-row/index.md)&gt;) |
| [isEmpty](is-empty.md) | [Kraken API]<br>open fun [isEmpty](is-empty.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [render](render.md) | [Kraken API]<br>open fun [render](render.md)(graphics: [Graphics2D](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Graphics2D.html)): [Dimension](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Dimension.html) |
| [setColumnAlignments](set-column-alignments.md) | [Kraken API]<br>open fun [setColumnAlignments](set-column-alignments.md)(alignments: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[TableAlignment](../-table-alignment/index.md)&gt;) |
| [setColumns](set-columns.md) | [Kraken API]<br>open fun [setColumns](set-columns.md)(elements: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[TableElement](../-table-element/index.md)&gt;)<br>open fun [setColumns](set-columns.md)(columns: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;) |
| [setRows](set-rows.md) | [Kraken API]<br>open fun [setRows](set-rows.md)(elements: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[TableRow](../-table-row/index.md)&gt;)<br>open fun [setRows](set-rows.md)(elements: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;&gt;) |
