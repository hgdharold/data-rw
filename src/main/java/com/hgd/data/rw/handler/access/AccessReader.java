package com.hgd.data.rw.handler.access;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.handler.AbstractReader;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 读取access文件
 *
 * @author hgd
 * @date 2020/04/26
 */
public class AccessReader extends AbstractReader<Row> {

    @Getter
    private Database database;
    private Table table;
    @Getter
    private int tableCount;
    @Getter
    private Set<String> tableNames;
    @Getter
    @Setter
    private String tableName;

    private AccessReader(Builder builder) {
        super(builder.file);
        this.tableName = builder.tableName;
    }

    @Override
    protected Closeable getOpenedResource() {
        return database;
    }

    private AccessReader init() throws IOException {
        database = new DatabaseBuilder().setAutoSync(false).setReadOnly(true).setFile(file).open();
        if (StringUtils.isNotBlank(tableName)) {
            table = database.getTable(tableName);
        } else {
            table = database.iterator().next();
        }
        if (table == null) {
            throw new NoSuchElementException("table not exist");
        }
        tableName = table.getName();
        tableNames = database.getTableNames();
        tableCount = tableNames.size();
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

    public static class Builder {
        private File file;
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

}
