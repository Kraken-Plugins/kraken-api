package com.kraken.api.overlay.table;

import lombok.Builder;
import lombok.Data;

import java.awt.*;

/**
 * A cell or column header of a {@link TableComponent}: its text, alignment and color. A null alignment or
 * color is replaced by the row, column or table default when the table is drawn.
 */
@Data
@Builder
public class TableElement {
    TableAlignment alignment;
    Color color;
    String content;
}
