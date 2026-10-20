package com.kraken.api.overlay.table;

import lombok.Builder;
import lombok.Data;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * A row of a {@link TableComponent} with its cells and optional row-wide color and alignment. Cells
 * without their own color or alignment use the row's values.
 */
@Data
@Builder
public class TableRow {
    Color rowColor;
    TableAlignment rowAlignment;
    @Builder.Default
    List<TableElement> elements = Collections.emptyList();
}
