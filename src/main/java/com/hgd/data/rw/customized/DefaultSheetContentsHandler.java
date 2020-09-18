package com.hgd.data.rw.customized;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * @author hgd
 * @date 2020/08/11
 */
public class DefaultSheetContentsHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

    protected int skipRows;
    protected int maxColumnNum;
    private BiConsumer<String[], Long> rowConsumer;
    private Runnable endCall;

    private int skipped = 0;
    private boolean skipCurrentRow = false;
    private String[] row;
    private final int capacity = 64;
    private int currentCol = 0;

    private long index;

    public DefaultSheetContentsHandler(int skipRows, int maxColumnNum, BiConsumer<String[], Long> rowConsumer, Runnable endCall) {
        this.skipRows = skipRows;
        this.maxColumnNum = maxColumnNum;
        this.rowConsumer = rowConsumer;
        this.endCall = endCall;
    }

    public DefaultSheetContentsHandler(int skipRows, int maxColumnNum, BiConsumer<String[], Long> rowConsumer) {
        this(skipRows, maxColumnNum, rowConsumer, null);
    }

    @Override
    public void startRow(int rowNum) {
        currentCol = 0;
        if (skipped < skipRows) {
            skipCurrentRow = true;
            skipped++;
        } else {
            skipCurrentRow = false;
            row = new String[capacity];
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
        if (!skipCurrentRow) {
            int colIndex = new CellReference(cellReference).getCol();
            currentCol = colIndex;
            if (maxColumnNum <= 0 || colIndex <= maxColumnNum - 1) {
                if (colIndex >= row.length) {
                    row = Arrays.copyOf(row, row.length + capacity);
                }
                row[colIndex] = formattedValue;
            }
        }
    }

    @Override
    public void endRow(int rowNum) {
        if (!skipCurrentRow) {
            if (row.length > currentCol + 1) {
                row = Arrays.copyOf(row, currentCol + 1);
            }
            rowConsumer.accept(row, index);
            index++;
        }
    }

    @Override
    public void endSheet() {
        if (endCall != null) {
            endCall.run();
        }
    }

}
