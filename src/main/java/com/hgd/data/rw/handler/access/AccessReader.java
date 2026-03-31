package com.hgd.data.rw.handler.access;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.handler.AbstractReader;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 读取access文件
 *
 * @author hgd
 * @date 2020/04/26
 */
public class AccessReader extends AbstractReader<Row> {

    private Database database;
    private Set<String> tableNames;
    private String tableName;
    private Table table;

    private final Map<String, String> columnNameTypeMap = new HashMap<>();

    private AccessReader(Builder builder) {
        super(builder.file);
        this.tableName = builder.tableName;
    }

    private AccessReader init() throws IOException {
        database = new DatabaseBuilder()
                .setAutoSync(false)
                .setReadOnly(true)
                .setIgnoreBrokenSystemCatalogIndex(true)
                .setFile(file).open();
        tableNames = database.getTableNames();

        if (StringUtils.isNotBlank(tableName)) {
            table = database.getTable(tableName);
        } else {
            // 默认第一个表
            table = database.iterator().next();
        }
        if (table == null) {
            throw new NoSuchElementException("table not exist");
        }
        tableName = table.getName();
        table.getColumns().forEach(col -> columnNameTypeMap.put(col.getName(), col.getType().name()));
        return this;
    }

    @Override
    protected Iterator<Row> genIterator() {
        return table.iterator();
    }

    public Iterator<Row> genIterator(String tableName) throws IOException {
        Table t = database.getTable(tableName);
        if (t == null) {
            return null;
        }
        this.tableName = t.getName();
        return t.iterator();
    }

    @Override
    public void close() throws IOException {
        if (database != null) {
            database.close();
        }
    }

    public static class Builder {
        private final File file;
        private String tableName;

        private Builder(File file) {
            this.file = file;
        }

        public Builder table(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public AccessReader build() throws IOException {
            AccessReader reader = null;
            try {
                reader = new AccessReader(this).init();
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

    public Database getDatabase() {
        return database;
    }

    public Set<String> getTableNames() {
        return tableNames;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, String> getColumnNameTypeMap() {
        return columnNameTypeMap;
    }

}
