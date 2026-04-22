package com.hgd.data.rw.handler.csv;

import com.hgd.data.rw.common.CharsetDetector.CharsetDetectionResult;
import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.handler.AbstractReader;
import com.opencsv.*;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;

import static com.hgd.data.rw.common.CharsetDetector.detectEncoding;
import static org.apache.commons.io.ByteOrderMark.*;

/**
 * 读取scv，可根据不同格式进行配置
 *
 * @author hgd
 * @date 2020/04/22
 */

public class CsvReader extends AbstractReader<String[]> {

    private static final Logger log = LoggerFactory.getLogger(CsvReader.class);

    private final InputStream inputStream;
    private final ICSVParser icsvParser;

    private CSVReader openCsvReader;

    private CsvReader(File file, ICSVParser icsvParser) {
        super(file);
        this.inputStream = null;
        this.icsvParser = icsvParser;
    }

    private CsvReader(InputStream inputStream, ICSVParser icsvParser) {
        super(null);
        this.inputStream = inputStream;
        this.icsvParser = icsvParser;
    }

    private static CsvReader createReader(
            File file,
            InputStream inputStream,
            ICSVParser icsvParser
    ) throws Exception {
        if (file == null && inputStream == null) {
            throw new IllegalArgumentException("file or inputStream must be not null");
        }
        if (icsvParser == null) {
            icsvParser = new RFC4180Parser();
        }
        CsvReader csvReader = null;
        if (file != null) {
            csvReader = new CsvReader(file, icsvParser);
        }
        if (inputStream != null) {
            csvReader = new CsvReader(inputStream, icsvParser);
        }

        try {
            return csvReader.init();
        } catch (Exception e) {
            Helper.closeQuietly(csvReader);
            throw e;
        }
    }

    public static CsvReader createReader(File file) throws Exception {
        return createReader(file, null, null);
    }

    public static CsvReader createReader(InputStream inputStream) throws Exception {
        return createReader(null, inputStream, null);
    }

    public static CsvReader createReader(File file, RFC4180ParserBuilder icsvParserBuilder) throws Exception {
        return createReader(file, null, icsvParserBuilder.build());
    }

    public static CsvReader createReader(InputStream inputStream, RFC4180ParserBuilder icsvParserBuilder) throws Exception {
        return createReader(null, inputStream, icsvParserBuilder.build());
    }

    public static CsvReader createReader(File file, CSVParserBuilder csvParserBuilder) throws Exception {
        return createReader(file, null, csvParserBuilder.build());
    }

    public static CsvReader createReader(InputStream inputStream, CSVParserBuilder csvParserBuilder) throws Exception {
        return createReader(null, inputStream, csvParserBuilder.build());
    }

    private CsvReader init() throws Exception {
        InputStreamReader isr;
        if (file != null) {
            CharsetDetectionResult detectedEncoding = detectEncoding(file);
            String charset = detectedEncoding.charset;
            if (!"UTF-8".equals(charset)) {
                log.warn("!!! detected file encoding is not UTF-8: {}", charset);
            }
            FileInputStream fis = new FileInputStream(file);
            BOMInputStream bomIs = BOMInputStream.builder()
                    .setInputStream(fis)
                    .setByteOrderMarks(
                            UTF_8,        // UTF-8 BOM: EF BB BF
                            UTF_16LE,     // UTF-16 LE BOM: FF FE
                            UTF_16BE,     // UTF-16 BE BOM: FE FF
                            UTF_32LE,     // UTF-32 LE BOM: FF FE 00 00
                            UTF_32BE      // UTF-32 BE BOM: 00 00 FE FF
                    )
                    .get();
            log.trace("file encoding: {}; bom: {}", charset, bomIs.getBOM());
            isr = new InputStreamReader(bomIs, charset);
        } else if (inputStream != null) {
            isr = new InputStreamReader(inputStream);
        } else {
            throw new Exception("file or inputStream must be not null");
        }
        openCsvReader = new CSVReaderBuilder(new BufferedReader(isr)).withCSVParser(icsvParser).build();
        iterator = new CSVIterator(openCsvReader);
        return this;
    }

    @Override
    protected Iterator<String[]> genIterator() {
        return iterator;
    }

    @Override
    public void close() throws IOException {
        if (openCsvReader != null) {
            openCsvReader.close();
        }
    }

}
