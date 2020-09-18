package com.hgd.data.rw.handler.csv;

import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.handler.AbstractReader;
import com.opencsv.*;

import java.io.*;
import java.util.Iterator;

/**
 * 读取scv，可根据不同格式进行配置
 *
 * @author hgd
 * @date 2020/04/22
 */

public class CsvReader extends AbstractReader<String[]> {

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
        BufferedReader bf = null;
        if (file != null) {
            bf = new BufferedReader(new FileReader(file));
        }
        if (inputStream != null) {
            bf = new BufferedReader(new InputStreamReader(inputStream));
        }
        openCsvReader = new CSVReaderBuilder(bf).withCSVParser(parser).build();
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
