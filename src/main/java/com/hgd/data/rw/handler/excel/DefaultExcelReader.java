package com.hgd.data.rw.handler.excel;

import com.hgd.data.rw.handler.excel.customized.DataFormatterPlainNumProxy;
import com.hgd.data.rw.handler.excel.customized.DefaultSheetContentsHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.Comments;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.util.Locale;

/**
 * @author hgd
 * @date 2020/08/11
 */
@Slf4j
public class DefaultExcelReader extends AbstractExcelReader<String[]> {

    private DefaultExcelReader(DefaultExcelReaderBuilder builder) {
        super(builder);
    }

    @Override
    protected DefaultHandler getContentHandler(StylesTable stylesTable, SharedStrings sharedStrings, Comments comments) {
        DataFormatter dataFormatter = new DataFormatter();
        Locale locale = getLocale();
        if (locale != null) {
            dataFormatter = new DataFormatter(locale);
        }
        try {
            dataFormatter = DataFormatterPlainNumProxy.createProxy(dataFormatter);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Failed to create proxy for DataFormatter", e);
        }
        return new XSSFSheetXMLHandler(stylesTable, comments, sharedStrings, getSheetContentsHandler(), dataFormatter, false);
    }

    protected SheetContentsHandler getSheetContentsHandler() {
        return new DefaultSheetContentsHandler(getSkipRows(), getMaxColumnNum(), getRowConsumer(), getEndCall());
    }

    public static class DefaultExcelReaderBuilder extends AbstractExcelReaderBuilder<
            DefaultExcelReader,
            DefaultExcelReaderBuilder> {

        private DefaultExcelReaderBuilder(File file) {
            super(file);
        }

        @Override
        protected DefaultExcelReaderBuilder self() {
            return this;
        }

        @Override
        protected DefaultExcelReader createReader() {
            return new DefaultExcelReader(this);
        }
    }

    public static DefaultExcelReaderBuilder builder(File file) {
        return new DefaultExcelReaderBuilder(file);
    }

}
