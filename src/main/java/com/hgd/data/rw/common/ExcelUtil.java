package com.hgd.data.rw.common;

import com.hgd.data.rw.customized.CustomXssfSheetXmlHandler;
import com.hgd.data.rw.customized.CustomXssfSheetXmlHandler.StyledCell;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hgd.data.rw.customized.CustomXssfSheetXmlHandler.SheetStyle;
import static com.hgd.data.rw.customized.CustomXssfSheetXmlHandler.StyledRow;

/**
 * Excel工具类
 *
 * @author hgd
 * @date 2020/4/29
 */
public class ExcelUtil {

    public static void addRow(Sheet sheet, Collection<String[]> rows) {
        int rowIndex = sheet.getLastRowNum() + 1;
        for (String[] rowData : rows) {
            addRow(sheet, rowData, rowIndex);
            rowIndex++;
        }
    }

    public static void addRow(Sheet sheet, String[] rowData) {
        int rowIndex = sheet.getLastRowNum() + 1;
        addRow(sheet, rowData, rowIndex);
    }

    public static void addRow(Sheet sheet, String[] rowData, int rowIndex) {
        if (sheet != null && rowData != null && rowData.length > 0) {
            Row row = sheet.createRow(rowIndex);
            for (int i = 0; i < rowData.length; i++) {
                CellUtil.createCell(row, i, rowData[i]);
            }
        }
    }

    /**
     * 复制StylesTable
     * workbook.getStylesSource().readFrom(excelReader.getXssfReader().getStylesData())不能达到预期的效果
     *
     * @param workbook      目标
     * @param oldStyleTable 原stylesable
     * @return 新旧index的对应关系，方便设置样式而不用新建样式或者对比样式(CellUtil不能准确的对比)
     */
    public static Map<Short, Short> cloneStylesTable(XSSFWorkbook workbook, StylesTable oldStyleTable) {
        Map<Short, Short> indexMap = new HashMap<>(64);
        int numCellStyles = oldStyleTable.getNumCellStyles();
        for (int i = 0; i < numCellStyles; i++) {
            XSSFCellStyle style = oldStyleTable.getStyleAt(i);
            XSSFCellStyle newStyle = workbook.createCellStyle();
            newStyle.cloneStyleFrom(style);
            indexMap.put(style.getIndex(), newStyle.getIndex());
        }
        return indexMap;
    }

    public static void addSheetStyle(Map<Short, Short> indexMap, Sheet sheet, SheetStyle
            sheetStyle, boolean addMergedRegion) {

        // 添加样式
        sheetStyle.getColumnStyle().forEach((k, v) -> {
            int newIndex = indexMap.get(v.getIndex());
            sheet.setDefaultColumnStyle(k, sheet.getWorkbook().getCellStyleAt(newIndex));
        });

        if (sheetStyle.getDefaultRowHeight() != null) {
            sheet.setDefaultRowHeightInPoints(sheetStyle.getDefaultRowHeight());
        }
        if (sheetStyle.getDefaultColumnWidth() != null) {
            sheet.setDefaultColumnWidth(sheetStyle.getDefaultColumnWidth());
        }
        sheetStyle.getColumnWidth().forEach((k, v) -> {
            ((XSSFSheet) sheet).getColumnHelper().setColWidth(k, v);
        });

        // 仅当行列号未变化时使用
        if (addMergedRegion) {
            for (CellRangeAddress cellRangeAddress : sheetStyle.getMergedRegions()) {
                sheet.addMergedRegion(cellRangeAddress);
            }
        }
    }

    public static void addSheetStyle(Map<Short, Short> indexMap, Sheet sheet, SheetStyle
            sheetStyle) {
        addSheetStyle(indexMap, sheet, sheetStyle, false);
    }

    public static void addRowWithStyle(Map<Short, Short> indexMap, Sheet sheet, StyledRow row) {
        addRowWithStyleToSheet(indexMap, sheet, row, row.getRowNum());
    }

    public static void addRowWithStyle(Map<Short, Short> indexMap, Sheet sheet, StyledRow row, int rowIndex) {
        addRowWithStyleToSheet(indexMap, sheet, row, rowIndex);
    }

    private static final String TRUE = "TRUE";

    private static void addRowWithStyleToSheet(Map<Short, Short> indexMap, Sheet sheet, StyledRow row, int rowIndex) {
        if (sheet != null && row != null && row.getCells().size() > 0) {
            Row newRow = sheet.createRow(rowIndex);
            if (row.getHeight() != null) {
                newRow.setHeightInPoints(row.getHeight());
            }
            if (row.getStyle() != null) {
                int newRowStyleIdx = indexMap.get(row.getStyle().getIndex());
                newRow.setRowStyle(sheet.getWorkbook().getCellStyleAt(newRowStyleIdx));
            }
            List<CustomXssfSheetXmlHandler.StyledCell> rowData = row.getCells();
            for (StyledCell cellInfo : rowData) {
                if (cellInfo != null) {
                    Cell cell = CellUtil.getCell(newRow, cellInfo.getCol());
                    String value = cellInfo.getValue();
                    switch (cellInfo.getCellType()) {
                        // >string
                        case _NONE:
                        case FORMULA:
                        case STRING:
                            cell.setCellValue(value);
                            break;
                        case ERROR:
                            cell.setCellErrorValue(Byte.parseByte(value));
                            break;
                        case BOOLEAN:
                            cell.setCellValue(value.equals(TRUE));
                            break;
                        case NUMERIC:
                            cell.setCellValue(Double.parseDouble(value));
                            break;
                        default: {
                        }

                    }
                    if (cellInfo.getStyle() != null) {
                        int newIndex = indexMap.get(cellInfo.getStyle().getIndex());
                        cell.setCellStyle(sheet.getWorkbook().getCellStyleAt(newIndex));
                    }
                }
            }
        }
    }

}
