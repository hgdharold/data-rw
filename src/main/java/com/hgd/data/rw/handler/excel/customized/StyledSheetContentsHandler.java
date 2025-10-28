package com.hgd.data.rw.handler.excel.customized;

import com.hgd.data.rw.handler.excel.customized.StyledEleDef.SheetStyle;
import com.hgd.data.rw.handler.excel.customized.StyledEleDef.StyledCell;
import com.hgd.data.rw.handler.excel.customized.StyledEleDef.StyledRow;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.ArrayList;
import java.util.function.BiConsumer;

/**
 * @author hgd
 * @date 2020/08/11
 */

@Slf4j
public class StyledSheetContentsHandler implements CustomXssfSheetXmlHandler.CustomSheetContentsHandler {

    private final int skipRows;
    private final int maxColumnNum;
    private final BiConsumer<StyledRow, Long> rowConsumer;
    private final Runnable endCall;

    private int skipped = 0;
    private boolean skipCurrentRow = false;
    private StyledRow row;
    private long index;
    @Getter
    private final SheetStyle sheetStyle = new SheetStyle();

    public StyledSheetContentsHandler(int skipRows, int maxColumnNum, BiConsumer<StyledRow, Long> rowConsumer, Runnable endCall) {
        this.skipRows = skipRows;
        this.maxColumnNum = maxColumnNum;
        this.rowConsumer = rowConsumer;
        this.endCall = endCall;
    }

    @Override
    public void startRow(int rowNum, CellStyle rowStyle, Short height) {
        if (skipped < skipRows) {
            skipCurrentRow = true;
            skipped++;
        } else {
            skipCurrentRow = false;
            row = new StyledRow().setRowNum(rowNum).setCells(new ArrayList<>());
            if (rowStyle != null) {
                row.setStyle(rowStyle);
            }
            if (height != null) {
                row.setHeight(height);
            }
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment, XSSFCellStyle cellStyle,
                     CellType cellType) {
        if (!skipCurrentRow) {
            int currentCol = new CellReference(cellReference).getCol();
            if (maxColumnNum <= 0 || currentCol <= maxColumnNum - 1) {
                row.getCells().add(new StyledCell()
                        .setCol(currentCol)
                        .setValue(formattedValue)
                        .setStyle(cellStyle)
                        .setCellType(cellType)
                );
            }
        }
    }

    @Override
    public void endRow(int rowNum, CellStyle rowStyle, Short height) {
        if (!skipCurrentRow) {
            rowConsumer.accept(row, index);
            index++;
        }
    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {
        log.trace("Header/Footer: text:{} isHeader:{} tagName:{}", text, isHeader, tagName);
    }

    @Override
    public void endSheetData() {
        log.trace("sheet data complete!");
    }

    @Override
    public void endSheet(SheetStyle sheetStyle) {
        this.sheetStyle.setMergedRegions(sheetStyle.getMergedRegions());
        if (endCall != null) {
            endCall.run();
        }
    }

}
