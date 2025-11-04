package com.hgd.data.rw.handler.csv;

import com.hgd.data.rw.handler.Writer;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hgd
 * @date 2020/7/10
 */
public class CsvWriter implements Writer<String[]> {

    private final File file;
    private final ICSVWriter writer;

    private boolean flush = true;
    private List<String> header = null;

    public CsvWriter(File file) throws IOException {
        this.file = file;
        BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
        // TODO builder 设置其他参数
        writer = new CSVWriterBuilder(bw).build();
    }

    @Override
    public void write(String[] item) throws IOException {
        writer.writeNext(item);
    }

    @Override
    public void write(Collection<String[]> items) throws IOException {
        writer.writeAll(items);
        if (flush) {
            writer.flush();
        }
    }

    public void addHeader(List<String> header) {
        if (this.header == null) {
            this.header = header;
            writer.writeNext(header.toArray(new String[0]));
        } else {
            throw new RuntimeException("header had been set,cannot be set again");
        }
    }

    public void writeMap(Collection<Map<String, Object>> items) throws IOException {
        if (header == null) {
            addHeader(new ArrayList<>(items.iterator().next().keySet()));
        }
        List<String[]> rows = items.parallelStream().map(i -> this.rowValues(i, header)).collect(Collectors.toList());
        writer.writeAll(rows);
        if (flush) {
            writer.flush();
        }
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private String[] rowValues(Map<String, Object> item, List<String> header) {
        return header.stream().map(f -> {
            Object srcVal = item.get(f);
            return srcVal == null ? null : srcVal.toString();
        }).toArray(String[]::new);
    }

    public File getFile() {
        return file;
    }

    public boolean isFlush() {
        return flush;
    }

    public void setFlush(boolean flush) {
        this.flush = flush;
    }

    public List<String> getHeader() {
        return header;
    }

}
