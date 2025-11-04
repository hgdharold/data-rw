package com.hgd.data.rw.handler.excel;

import com.hgd.data.rw.handler.excel.customized.CustomXssfSheetXmlHandler;
import com.hgd.data.rw.handler.excel.customized.CustomXssfSheetXmlHandler.CustomSheetContentsHandler;
import com.hgd.data.rw.handler.excel.customized.DataFormatterPlainNumProxy;
import com.hgd.data.rw.handler.excel.customized.StyledEleDef.SheetStyle;
import com.hgd.data.rw.handler.excel.customized.StyledEleDef.StyledRow;
import com.hgd.data.rw.handler.excel.customized.StyledSheetContentsHandler;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.model.Comments;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;

/**
 * @author hgd
 * @date 2020/08/11
 */

public class StyledExcelReader extends AbstractExcelReader<StyledRow> {

    private static final Logger log = LoggerFactory.getLogger(StyledExcelReader.class);

    private SheetStyle sheetStyle = new SheetStyle();

    private StyledExcelReader(StyledExcelReaderBuilder builder) {
        super(builder);
    }

    @Override
    protected DefaultHandler getContentHandler(StylesTable stylesTable, SharedStrings sharedStrings, Comments comments) {
        DataFormatter dataFormatter = new DataFormatter(getLocale());
        try {
            dataFormatter = DataFormatterPlainNumProxy.createProxy(dataFormatter);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Failed to create proxy for DataFormatter", e);
        }
        return new CustomXssfSheetXmlHandler(stylesTable, comments, sharedStrings, getSheetContentsHandler(), dataFormatter, false);
    }

    protected CustomSheetContentsHandler getSheetContentsHandler() {
        StyledSheetContentsHandler sheetHandler = new StyledSheetContentsHandler(getSkipRows(), getMaxColumnNum(), getRowConsumer(), getEndCall());
        sheetStyle = sheetHandler.getSheetStyle();
        return sheetHandler;
    }

    public static class StyledExcelReaderBuilder extends AbstractExcelReaderBuilder<
            StyledExcelReader,
            StyledExcelReaderBuilder> {

        private StyledExcelReaderBuilder(File file) {
            super(file);
        }

        @Override
        protected StyledExcelReaderBuilder self() {
            return this;
        }

        @Override
        protected StyledExcelReader createReader() {
            return new StyledExcelReader(this);
        }
    }

    public static StyledExcelReaderBuilder builder(File file) {
        return new StyledExcelReaderBuilder(file);
    }

    public SheetStyle getSheetStyle() {
        return sheetStyle;
    }

}
