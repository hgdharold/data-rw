package com.hgd.data.rw.handler.excel;

import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.customized.DefaultSheetContentsHandler;
import com.hgd.data.rw.customized.PlainNumFormatterCglibProxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

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
    protected DefaultHandler getContentHandler(StylesTable stylesTable, SharedStrings sharedStrings) {
        DataFormatter dataFormatter = new DataFormatter(locale);
        PlainNumFormatterCglibProxy numFormatterCglibProxy = new PlainNumFormatterCglibProxy(dataFormatter);
        return new XSSFSheetXMLHandler(stylesTable, sharedStrings, getSheetContentsHandler(),
                numFormatterCglibProxy.createProxy(), false);
    }

    protected XSSFSheetXMLHandler.SheetContentsHandler getSheetContentsHandler() {
        return new DefaultSheetContentsHandler(skipRows, maxColumnNum, rowConsumer, endCall);
    }

    /**
     * 同步方法，在解析完成后才会返回
     *
     * @return Iterator 注意，在解析发生异常时，会返回null
     */
    @Override
    protected Iterator<String[]> genIterator() {
        // new data storage
        List<String[]> storage = new ArrayList<>();
        // set default callback
        setRowConsumer(new DefaultRowConsumer(storage));
        try {
            parse();
        } catch (IOException | SAXException e) {
            return null;
        }
        return storage.iterator();
    }

    public static class DefaultExcelReaderBuilder extends AbstractExcelReaderBuilder<DefaultExcelReaderBuilder> {

        private DefaultExcelReaderBuilder(File file) {
            super(file);
        }

        public DefaultExcelReader build() throws Exception {
            DefaultExcelReader reader = null;
            try {
                reader = new DefaultExcelReader(this);
                reader.init();
                return reader;
            } catch (Exception e) {
                Helper.closeQuietly(reader);
                throw e;
            }
        }
    }

    public static DefaultExcelReaderBuilder builder(File file) {
        return new DefaultExcelReaderBuilder(file);
    }

    private static final class DefaultRowConsumer implements BiConsumer<String[], Long> {

        private List<String[]> storage;

        private DefaultRowConsumer(List<String[]> storage) {
            this.storage = storage;
        }

        @Override
        public void accept(String[] row, Long idx) {
            storage.add(row);
        }
    }

}
