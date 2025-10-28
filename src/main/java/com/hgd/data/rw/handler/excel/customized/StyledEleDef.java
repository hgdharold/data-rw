package com.hgd.data.rw.handler.excel.customized;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;
import java.util.Map;

public class StyledEleDef {

    @Data
    @Accessors(chain = true)
    public static class StyledCell {
        // 0-based
        private int col;
        private String value;
        private CellStyle style;
        private CellType cellType;
    }

    @Data
    @Accessors(chain = true)
    public static class StyledRow {
        // 0-based
        private int rowNum;
        private Short height;
        private CellStyle style;
        private List<StyledCell> cells;
    }

    @Data
    @Accessors(chain = true)
    public static class SheetStyle {
        private Integer defaultColumnWidth;
        private Float defaultRowHeight;
        private Map<Integer, Float> columnWidth;
        private Map<Integer, CellStyle> columnStyle;
        private List<CellRangeAddress> mergedRegions;
    }
}
