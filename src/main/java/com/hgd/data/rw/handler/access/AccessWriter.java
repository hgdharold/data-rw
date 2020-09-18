package com.hgd.data.rw.handler.access;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import com.hgd.data.rw.common.AccessUtil;
import com.hgd.data.rw.handler.Writer;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author hgd
 * @date 2020/7/10
 */

@Slf4j
public class AccessWriter implements Writer<Map<String, Object>> {

    private Table table;
    private boolean flush = true;

    public AccessWriter(Table table) {
        this.table = table;
    }

    @Override
    public void write(Map<String, Object> item) throws IOException {
        table.addRowFromMap(item);
    }

    @Override
    public void write(Collection<Map<String, Object>> items) throws IOException {
        List<Object[]> rowValuesList = new ArrayList<>(items.size());
        for (Map<String, Object> row : items) {
            rowValuesList.add(table.asRow(row));
        }
        table.addRows(rowValuesList);
        if (flush) {
            table.getDatabase().flush();
        }
    }

    @Override
    public void flush() throws IOException {
        table.getDatabase().flush();
    }

    @Override
    public void close() throws IOException {
        table.getDatabase().close();
    }

    public Table getTable() {
        return table;
    }

    public boolean isFlush() {
        return flush;
    }

    public void setFlush(boolean flush) {
        this.flush = flush;
    }

    public static Database cloneDb(File srcFile, File dstFile) throws IOException {
        Database oldDb = AccessUtil.getReadOnlyDatabase(srcFile);
        if (dstFile.exists()) {
            try {
                Database db = DatabaseBuilder.open(dstFile);
                return db;
            } catch (IOException e) {
                log.error("file exists,but not access file: {}", dstFile.getPath(), e);
                return null;
            }
        }
        Database newDb = AccessUtil.cloneEmptyDatabase(oldDb, dstFile);
        oldDb.close();
        return newDb;
    }
}
