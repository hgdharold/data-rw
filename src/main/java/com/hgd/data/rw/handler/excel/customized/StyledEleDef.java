package com.hgd.data.rw.handler.excel.customized;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;
import java.util.Map;

public class StyledEleDef {

    public static class StyledCell {
        // 0-based
        private int col;
        private String value;
        private CellStyle style;
        private CellType cellType;

        public int getCol() {
            return col;
        }

        public StyledCell setCol(int col) {
            this.col = col;
            return this;
        }

        public String getValue() {
            return value;
        }

        public StyledCell setValue(String value) {
            this.value = value;
            return this;
        }

        public CellStyle getStyle() {
            return style;
        }

        public StyledCell setStyle(CellStyle style) {
            this.style = style;
            return this;
        }

        public CellType getCellType() {
            return cellType;
        }

        public StyledCell setCellType(CellType cellType) {
            this.cellType = cellType;
            return this;
        }
    }

    public static class StyledRow {
        public int getRowNum() {
            return rowNum;
        }

        public StyledRow setRowNum(int rowNum) {
            this.rowNum = rowNum;
            return this;
        }

        public Short getHeight() {
            return height;
        }

        public StyledRow setHeight(Short height) {
            this.height = height;
            return this;
        }

        public CellStyle getStyle() {
            return style;
        }

        public StyledRow setStyle(CellStyle style) {
            this.style = style;
            return this;
        }

        public List<StyledCell> getCells() {
            return cells;
        }

        public StyledRow setCells(List<StyledCell> cells) {
            this.cells = cells;
            return this;
        }

        // 0-based
        private int rowNum;
        private Short height;
        private CellStyle style;
        private List<StyledCell> cells;
    }

    public static class SheetStyle {
        private Integer defaultColumnWidth;
        private Float defaultRowHeight;
        private Map<Integer, Float> columnWidth;
        private Map<Integer, CellStyle> columnStyle;
        private List<CellRangeAddress> mergedRegions;

        public Integer getDefaultColumnWidth() {
            return defaultColumnWidth;
        }

        public SheetStyle setDefaultColumnWidth(Integer defaultColumnWidth) {
            this.defaultColumnWidth = defaultColumnWidth;
            return this;
        }

        public Float getDefaultRowHeight() {
            return defaultRowHeight;
        }

        public SheetStyle setDefaultRowHeight(Float defaultRowHeight) {
            this.defaultRowHeight = defaultRowHeight;
            return this;
        }

        public Map<Integer, Float> getColumnWidth() {
            return columnWidth;
        }

        public SheetStyle setColumnWidth(Map<Integer, Float> columnWidth) {
            this.columnWidth = columnWidth;
            return this;
        }

        public Map<Integer, CellStyle> getColumnStyle() {
            return columnStyle;
        }

        public SheetStyle setColumnStyle(Map<Integer, CellStyle> columnStyle) {
            this.columnStyle = columnStyle;
            return this;
        }

        public List<CellRangeAddress> getMergedRegions() {
            return mergedRegions;
        }

        public SheetStyle setMergedRegions(List<CellRangeAddress> mergedRegions) {
            this.mergedRegions = mergedRegions;
            return this;
        }
    }
}
