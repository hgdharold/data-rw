package com.hgd.data.rw.handler.excel;

import com.hgd.data.rw.handler.Writer;
import com.hgd.data.rw.handler.excel.customized.ExcelUtil;
import com.hgd.data.rw.handler.excel.customized.StyledEleDef.SheetStyle;
import com.hgd.data.rw.handler.excel.customized.StyledEleDef.StyledRow;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author hgd
 * @date 2020/7/28
 * TODO 待测试
 */
public class StyledExcelWriter implements Writer<StyledRow> {

    private final XSSFSheet sheet;
    private final StylesTable stylesTable;
    private final SheetStyle sheetStyle;
    private final Map<Short, Short> styleIndexMap;

    public StyledExcelWriter(XSSFSheet sheet, StylesTable stylesTable, SheetStyle sheetStyle) {
        this.sheet = sheet;
        this.stylesTable = stylesTable;
        this.sheetStyle = sheetStyle;
        styleIndexMap = ExcelUtil.cloneStylesTable(sheet.getWorkbook(), stylesTable);
    }

    @Override
    public void write(StyledRow item) throws IOException {
        ExcelUtil.addRowWithStyle(styleIndexMap, sheet, item);
    }

    @Override
    public void write(Collection<StyledRow> items) throws IOException {
        for (StyledRow item : items) {
            write(item);
        }
    }

    @Override
    public void flush() throws IOException {
        sheet.getWorkbook().getPackage().flush();
    }

    @Override
    public void close() throws IOException {
        sheet.getWorkbook().getPackage().close();
    }
}
