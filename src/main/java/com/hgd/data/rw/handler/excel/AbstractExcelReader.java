package com.hgd.data.rw.handler.excel;

import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.handler.AbstractReader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * sax读取excel文件 仅限xlsx
 * <p>
 * 同步使用方式
 * 异步使用方式
 *
 * @author hgd
 * @date 2020/4/23
 */
@Slf4j
public abstract class AbstractExcelReader<T> extends AbstractReader<T> {

    protected final int skipRows;
    protected final int maxColumnNum;
    @Getter
    protected int sheetIndex;
    @Getter
    protected String sheetName;
    @Getter
    private int sheetCount;
    @Getter
    private StylesTable stylesTable;
    private SharedStrings sharedStringsTable;

    private OPCPackage pkg;
    protected InputSource sheetSource;
    protected XMLReader xmlReader;

    protected AtomicBoolean started = new AtomicBoolean(false);

    @Setter
    protected BiConsumer<T, Long> rowConsumer;
    @Setter
    protected Runnable endCall;

    protected AbstractExcelReader(AbstractBuilder builder) {
        super(builder.file);
        this.skipRows = builder.skipRows;
        this.maxColumnNum = builder.maxColumnNum;
        this.sheetIndex = builder.sheetIndex;
        this.sheetName = builder.sheetName;
    }

    protected void init() throws Exception {
        pkg = OPCPackage.open(file, PackageAccess.READ);
        XSSFReader xssfReader = new XSSFReader(pkg);
        stylesTable = xssfReader.getStylesTable();
        sharedStringsTable = new ReadOnlySharedStringsTable(pkg);
        // decide which sheet
        XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        InputStream sheetStream = null;
        List<String> names = new ArrayList<>();
        List<InputStream> streams = new ArrayList<>();
        while (sheetIterator.hasNext()) {
            InputStream next = sheetIterator.next();
            names.add(sheetIterator.getSheetName());
            streams.add(next);
        }
        sheetCount = names.size();
        // 设定了sheetIndex值
        if (sheetIndex != 0) {
            if (sheetIndex < 0 || sheetIndex >= sheetCount) {
                throw new IllegalArgumentException("incorrect sheetIndex");
            }
            sheetName = names.get(sheetIndex);
            sheetStream = streams.get(sheetIndex);
        }
        // 设定了sheetName
        else if (sheetName != null) {
            int idx = names.indexOf(sheetName);
            if (idx == -1) {
                throw new IllegalArgumentException("incorrect sheetName");
            }
            sheetIndex = idx;
            sheetStream = streams.get(idx);
        } else {
            sheetName = names.get(0);
            sheetStream = streams.get(0);
        }
        // create xml reader
        xmlReader = XMLReaderFactory.createXMLReader();
        sheetSource = new InputSource(sheetStream);
    }

    /**
     * customize ContentHandler
     *
     * @param stylesTable
     * @param sharedStrings
     * @return
     */
    protected abstract DefaultHandler getContentHandler(StylesTable stylesTable, SharedStrings sharedStrings);

    /**
     * 启动解析，同步方法，可放入单独的线程中。
     */
    public void parse() throws IOException, SAXException {
        if (rowConsumer == null) {
            return;
        }
        synchronized (this) {
            if (!started.get()) {
                log.info("开始解析：{}", file.getPath());
                started.set(true);
                this.notifyAll();
            } else {
                return;
            }
        }
        xmlReader.setContentHandler(getContentHandler(stylesTable, sharedStringsTable));
        try {
            xmlReader.parse(sheetSource);
        } catch (IOException | SAXException e) {
            log.warn("解析excel文件出错: {}", file.getPath(), e);
            throw e;
        } finally {
            Helper.closeQuietly(sheetSource.getByteStream());
        }
    }

    @Override
    protected Closeable getOpenedResource() {
        return pkg;
    }

    @Override
    public void close() throws IOException {
        pkg.close();
    }

    @SuppressWarnings("unchecked")
    public static abstract class AbstractBuilder<T extends AbstractBuilder<T>> {
        protected File file;
        private int skipRows = 0;
        private int maxColumnNum;
        private int sheetIndex = 0;
        private String sheetName;

        protected AbstractBuilder(File file) {
            this.file = file;
        }

        /**
         * 设定读取第几个sheet，从0开始，优先于sheetName
         */
        public T sheetIndex(int sheetIndex) {
            this.sheetIndex = sheetIndex;
            this.sheetName = null;
            return (T) this;
        }

        /**
         * 设定要读取的sheet
         */
        public T sheetName(String sheetName) {
            this.sheetName = sheetName;
            return (T) this;
        }

        /**
         * 设定跳过的起始行数，空行不算
         */
        public T skipRows(int skipRows) {
            this.skipRows = skipRows;
            return (T) this;
        }

        /**
         * 设定读取的最大列数
         */
        public T maxColumnNum(int maxColumnNum) {
            this.maxColumnNum = maxColumnNum;
            return (T) this;
        }

    }
}
