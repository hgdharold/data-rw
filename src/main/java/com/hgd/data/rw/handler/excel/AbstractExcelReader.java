package com.hgd.data.rw.handler.excel;

import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.handler.AbstractReader;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.Comments;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
public abstract class AbstractExcelReader<T> extends AbstractReader<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractExcelReader.class);

    private final Locale locale;
    private final int skipRows;
    private final int maxColumnNum;
    // 下面两个参数确定读取哪一个sheet
    private int sheetIndex;
    private String sheetName;
    // 下面两个参数可以在开始解析之前设置
    private BiConsumer<T, Long> rowConsumer;
    private Runnable endCall;

    private OPCPackage pkg;
    private StylesTable stylesTable;
    private SharedStrings sharedStringsTable;
    private int sheetCount;
    private Comments comments;
    private InputSource sheetSource;
    private XMLReader xmlReader;

    protected AtomicBoolean started = new AtomicBoolean(false);

    protected AbstractExcelReader(AbstractExcelReaderBuilder<?, ?> builder) {
        super(builder.file);
        this.locale = builder.locale;
        this.skipRows = builder.skipRows;
        this.maxColumnNum = builder.maxColumnNum;
        this.sheetIndex = builder.sheetIndex;
        this.sheetName = builder.sheetName;
    }

    /**
     * 可能抛出异常的初始化操作：读取文件，获取全局属性，确定读取的sheet
     */
    protected void init() throws Exception {
        ZipSecureFile.setMinInflateRatio(0.005);
        pkg = OPCPackage.open(file, PackageAccess.READ);
        XSSFReader xssfReader = new XSSFReader(pkg);
        stylesTable = xssfReader.getStylesTable();
        sharedStringsTable = new ReadOnlySharedStringsTable(pkg);

        // 遍历得到所有的sheet的相关信息
        XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        List<String> names = new ArrayList<>();
        List<Comments> commentsList = new ArrayList<>();
        List<InputStream> streams = new ArrayList<>();
        while (sheetIterator.hasNext()) {
            InputStream next = sheetIterator.next();
            names.add(sheetIterator.getSheetName());
            commentsList.add(sheetIterator.getSheetComments());
            streams.add(next);
        }
        sheetCount = names.size();

        // 决定读取哪一个sheet
        InputStream sheetStream = null;
        // 如果设定了sheetIndex值
        if (sheetIndex != 0) {
            if (sheetIndex < 0 || sheetIndex >= sheetCount) {
                throw new IllegalArgumentException("incorrect sheetIndex");
            }
            sheetName = names.get(sheetIndex);
            comments = commentsList.get(sheetIndex);
            sheetStream = streams.get(sheetIndex);
        }
        // 如果设定了sheetName
        else if (sheetName != null) {
            sheetIndex = names.indexOf(sheetName);
            if (sheetIndex == -1) {
                throw new IllegalArgumentException("incorrect sheetName");
            }
            comments = commentsList.get(sheetIndex);
            sheetStream = streams.get(sheetIndex);
        }
        // 默认读取第一个sheet
        else {
            sheetName = names.get(0);
            comments = commentsList.get(0);
            sheetStream = streams.get(0);
        }
        // 关闭其余的流
        for (int i = 0; i < streams.size(); i++) {
            if (i != sheetIndex) {
                streams.get(i).close();
            }
        }
        // 目标
        sheetSource = new InputSource(sheetStream);

        // create xml reader
        xmlReader = XMLReaderFactory.createXMLReader();
    }

    /**
     * 启动解析，同步方法，可放入单独的线程中。
     */
    public void parse() throws IOException, SAXException {
        if (rowConsumer == null) {
            return;
        }
        synchronized (this) {
            if (!started.get()) {
                log.trace("开始解析：{}", file.getPath());
                started.set(true);
                this.notifyAll();
            } else {
                return;
            }
        }
        xmlReader.setContentHandler(getContentHandler(stylesTable, sharedStringsTable, comments));
        try {
            xmlReader.parse(sheetSource);
        } catch (IOException | SAXException e) {
            log.warn("解析excel文件出错: {}", file.getPath(), e);
            throw e;
        } finally {
            Helper.closeQuietly(sheetSource.getByteStream());
        }
    }

    /**
     * customize ContentHandler
     *
     * @param stylesTable   格式索引表
     * @param sharedStrings 共享字符串表
     * @return 数据处理回调
     */
    protected abstract DefaultHandler getContentHandler(StylesTable stylesTable, SharedStrings sharedStrings, Comments comments);

    @Override
    public void close() throws IOException {
        if (pkg != null) {
            pkg.close();
        }
    }

    /**
     * 同步方法，在解析完成后才会返回
     *
     * @return Iterator 注意，在解析发生异常时，会返回null
     */
    @Override
    protected Iterator<T> genIterator() {
        // new data storage
        List<T> storage = new ArrayList<>();
        // set default callback
        setRowConsumer((row, idx) -> {
            storage.add(row);
        });
        try {
            parse();
        } catch (IOException | SAXException e) {
            return null;
        }
        return storage.iterator();
    }

    /**
     *
     */
    public static abstract class AbstractExcelReaderBuilder<
            R extends AbstractExcelReader<?>,
            B extends AbstractExcelReaderBuilder<R, B>> {
        protected final File file;
        private Locale locale;
        private int skipRows = 0;
        private int maxColumnNum;
        private int sheetIndex = 0;
        private String sheetName;

        protected AbstractExcelReaderBuilder(File file) {
            this.file = file;
        }

        /**
         * 设定Locale
         */
        public B locale(Locale locale) {
            this.locale = locale;
            return self();
        }

        /**
         * 设定读取第几个sheet，从0开始，优先于sheetName
         */
        public B sheetIndex(int sheetIndex) {
            this.sheetIndex = sheetIndex;
            this.sheetName = null;
            return self();
        }

        /**
         * 设定要读取的sheet
         */
        public B sheetName(String sheetName) {
            this.sheetName = sheetName;
            return self();
        }

        /**
         * 设定跳过的起始行数，空行不算
         */
        public B skipRows(int skipRows) {
            this.skipRows = skipRows;
            return self();
        }

        /**
         * 设定读取的最大列数
         */
        public B maxColumnNum(int maxColumnNum) {
            this.maxColumnNum = maxColumnNum;
            return self();
        }

        public R build() throws Exception {
            R reader = null;
            try {
                reader = createReader();
                reader.init();
                return reader;
            } catch (Exception e) {
                Helper.closeQuietly(reader);
                throw e;
            }
        }

        protected abstract B self();

        protected abstract R createReader();

    }

    public Locale getLocale() {
        return locale;
    }

    public int getSkipRows() {
        return skipRows;
    }

    public int getMaxColumnNum() {
        return maxColumnNum;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }

    public String getSheetName() {
        return sheetName;
    }

    public BiConsumer<T, Long> getRowConsumer() {
        return rowConsumer;
    }

    public AbstractExcelReader<T> setRowConsumer(BiConsumer<T, Long> rowConsumer) {
        this.rowConsumer = rowConsumer;
        return this;
    }

    public Runnable getEndCall() {
        return endCall;
    }

    public AbstractExcelReader<T> setEndCall(Runnable endCall) {
        this.endCall = endCall;
        return this;
    }

    public StylesTable getStylesTable() {
        return stylesTable;
    }

    public int getSheetCount() {
        return sheetCount;
    }
}
