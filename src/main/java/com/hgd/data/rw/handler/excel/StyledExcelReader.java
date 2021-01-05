package com.hgd.data.rw.handler.excel;

import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.customized.CustomXssfSheetXmlHandler;
import com.hgd.data.rw.customized.CustomXssfSheetXmlHandler.SheetStyle;
import com.hgd.data.rw.customized.CustomXssfSheetXmlHandler.StyledRow;
import com.hgd.data.rw.customized.PlainNumFormatterCglibProxy;
import com.hgd.data.rw.customized.StyledSheetContentsHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
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
public class StyledExcelReader extends AbstractExcelReader<StyledRow> {

    @Getter
    private SheetStyle sheetStyle = new SheetStyle();

    private StyledExcelReader(Builder builder) {
        super(builder);
    }

    @Override
    protected Iterator<StyledRow> genIterator() {
        // new data storage
        List<StyledRow> storage = new ArrayList<>();
        // set default callback
        setRowConsumer(new DefaultRowConsumer(storage));
        try {
            parse();
        } catch (IOException | SAXException e) {
            return null;
        }
        return storage.iterator();
    }

    @Override
    protected DefaultHandler getContentHandler(StylesTable stylesTable, SharedStrings sharedStrings) {
        DataFormatter dataFormatter = new DataFormatter();
        PlainNumFormatterCglibProxy numFormatterCglibProxy = new PlainNumFormatterCglibProxy(dataFormatter);
        return new CustomXssfSheetXmlHandler(stylesTable, sharedStrings, getSheetContentsHandler(),
                numFormatterCglibProxy.createProxy(), false);
    }

    protected CustomXssfSheetXmlHandler.SheetContentsHandler getSheetContentsHandler() {
        StyledSheetContentsHandler sheetHandler = new StyledSheetContentsHandler(skipRows, maxColumnNum, rowConsumer, endCall);
        sheetStyle = sheetHandler.getSheetStyle();
        return sheetHandler;
    }

    public static class Builder extends AbstractBuilder<Builder> {

        private Builder(File file) {
            super(file);
        }

        public StyledExcelReader build() throws Exception {
            StyledExcelReader reader = null;
            try {
                reader = new StyledExcelReader(this);
                reader.init();
                return reader;
            } catch (Exception e) {
                Helper.closeQuietly(reader);
                throw e;
            }
        }
    }

    public static Builder builder(File file) {
        return new Builder(file);
    }

    private static final class DefaultRowConsumer implements BiConsumer<StyledRow, Long> {

        private List<StyledRow> storage;

        private DefaultRowConsumer(List<StyledRow> storage) {
            this.storage = storage;
        }

        @Override
        public void accept(StyledRow row, Long idx) {
            storage.add(row);
        }
    }

}
