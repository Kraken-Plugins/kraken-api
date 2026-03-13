//[kraken-api](../../../index.md)/[com.kraken.api.query.container.inventory](../index.md)/[InventoryOrder](index.md)

# InventoryOrder

[Kraken API]\
enum [InventoryOrder](index.md)

## Entries

| | |
|---|---|
| [TOP_LEFT_BOTTOM_RIGHT](-t-o-p_-l-e-f-t_-b-o-t-t-o-m_-r-i-g-h-t/index.md) | [Kraken API]<br>[TOP_LEFT_BOTTOM_RIGHT](-t-o-p_-l-e-f-t_-b-o-t-t-o-m_-r-i-g-h-t/index.md)<br>Standard reading order: Row 1 (Left-&gt;Right), Row 2 (Left-&gt;Right)... |
| [BOTTOM_RIGHT_TOP_LEFT](-b-o-t-t-o-m_-r-i-g-h-t_-t-o-p_-l-e-f-t/index.md) | [Kraken API]<br>[BOTTOM_RIGHT_TOP_LEFT](-b-o-t-t-o-m_-r-i-g-h-t_-t-o-p_-l-e-f-t/index.md)<br>Reverse reading order: Last Item -&gt; First Item. |
| [TWO_ROW_LEFT_RIGHT](-t-w-o_-r-o-w_-l-e-f-t_-r-i-g-h-t/index.md) | [Kraken API]<br>[TWO_ROW_LEFT_RIGHT](-t-w-o_-r-o-w_-l-e-f-t_-r-i-g-h-t/index.md)<br>Drops two rows at a time starting from left to right. Sequence: (R0,C0)-&gt;(R1,C0) then (R0,C1)-&gt;(R1,C1)... |
| [ZIG_ZAG](-z-i-g_-z-a-g/index.md) | [Kraken API]<br>[ZIG_ZAG](-z-i-g_-z-a-g/index.md)<br>Snake/Zig-Zag pattern: Row 1 (Left-&gt;Right), Row 2 (Right-&gt;Left), Row 3 (Left-&gt;Right)... Reduces mouse travel distance significantly. |
| [ZIG_ZAG_REVERSE](-z-i-g_-z-a-g_-r-e-v-e-r-s-e/index.md) | [Kraken API]<br>[ZIG_ZAG_REVERSE](-z-i-g_-z-a-g_-r-e-v-e-r-s-e/index.md)<br>Reverse Snake/Zig-Zag pattern starting from the bottom right. |
| [TOP_DOWN_LEFT_RIGHT](-t-o-p_-d-o-w-n_-l-e-f-t_-r-i-g-h-t/index.md) | [Kraken API]<br>[TOP_DOWN_LEFT_RIGHT](-t-o-p_-d-o-w-n_-l-e-f-t_-r-i-g-h-t/index.md)<br>Vertical columns: Col 1 (Top-&gt;Bottom), Col 2 (Top-&gt;Bottom)... |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [InventoryOrder](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[InventoryOrder](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
