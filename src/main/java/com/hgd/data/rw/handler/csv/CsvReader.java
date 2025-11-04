package com.hgd.data.rw.handler.csv;

import com.hgd.data.rw.common.CharsetDetector.CharsetDetectionResult;
import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.handler.AbstractReader;
import com.opencsv.*;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
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

    private final char separator;
    private final boolean ignoreQuotations;
    private final boolean rfc4180;

    private final InputStream inputStream;
    private CSVReader openCsvReader;

    private CsvReader(Builder builder) {
        super(builder.file);
        this.separator = builder.separator;
        this.ignoreQuotations = builder.ignoreQuotations;
        this.rfc4180 = builder.rfc4180;
        this.inputStream = builder.inputStream;
    }

    @Override
    protected Closeable getOpenedResource() {
        return openCsvReader;
    }

    private CsvReader init() throws Exception {
        ICSVParser parser = rfc4180 ? new RFC4180ParserBuilder().withSeparator(separator).build()
                : new CSVParserBuilder().withSeparator(separator).withIgnoreQuotations(ignoreQuotations).build();
        InputStreamReader isr;
        if (file != null) {
            CharsetDetectionResult detectedEncoding = detectEncoding(file);
            Charset fileEncoding = Charset.forName(detectedEncoding.charset);
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
            log.trace("file encoding: {}; bom: {}", fileEncoding.name(), bomIs.getBOM());
            isr = new InputStreamReader(bomIs, fileEncoding);
        } else if (inputStream != null) {
            isr = new InputStreamReader(inputStream);
        } else {
            throw new Exception("file or inputStream must be not null");
        }
        openCsvReader = new CSVReaderBuilder(new BufferedReader(isr)).withCSVParser(parser).build();
        iterator = new CSVIterator(openCsvReader);
        return this;
    }

    @Override
    protected Iterator<String[]> genIterator() {
        return iterator;
    }

    public static class Builder {
        private File file;
        private InputStream inputStream;
        private char separator = ICSVParser.DEFAULT_SEPARATOR;
        private boolean ignoreQuotations = false;
        private boolean rfc4180 = false;

        private Builder(File file) {
            this.file = file;
        }

        private Builder(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public Builder separator(char separator) {
            this.separator = separator;
            return this;
        }

        public Builder ignoreQuotations(boolean ignoreQuotations) {
            this.ignoreQuotations = ignoreQuotations;
            return this;
        }

        public Builder rfc4180(boolean rfc4180) {
            this.rfc4180 = rfc4180;
            return this;
        }

        public CsvReader build() throws Exception {
            CsvReader reader = null;
            try {
                reader = new CsvReader(this).init();
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

    public static Builder builder(InputStream inputStream) {
        return new Builder(inputStream);
    }

}
