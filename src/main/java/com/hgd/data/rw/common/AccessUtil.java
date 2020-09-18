package com.hgd.data.rw.common;

import com.healthmarketscience.jackcess.*;

import java.io.File;
import java.io.IOException;

/**
 * Access工具类
 *
 * @author hgd
 * @date 2020-04-06
 */
public class AccessUtil {

    /**
     * 只读数据库 提高速度
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static Database getReadOnlyDatabase(File file) throws IOException {
        return new DatabaseBuilder().setAutoSync(false).setReadOnly(true).setFile(file).open();
    }

    /**
     * 复制数据库和表，但不包含数据
     *
     * @param sourceDb
     * @param file
     * @return
     * @throws IOException
     */
    public static Database cloneEmptyDatabase(Database sourceDb, File file) throws IOException {
        if (!file.exists()) {
            boolean success = file.createNewFile();
            if (!success) {
                throw new IOException("create file failed ");
            }
        }
        Database newDb = new DatabaseBuilder(file).setAutoSync(false).setFileFormat(sourceDb.getFileFormat()).create();
        for (Table table : sourceDb) {
            TableBuilder tBuilder = new TableBuilder(table.getName());
            for (Column col : table.getColumns()) {
                tBuilder.addColumn(new ColumnBuilder(col.getName()).setFromColumn(col).setMaxLength());
            }
            tBuilder.toTable(newDb);
        }
        return newDb;
    }

    /**
     * 向表中添加列
     *
     * @param table
     * @param name
     * @param dataType
     * @throws IOException
     */
    public static void addColumnToTable(Table table, String name, DataType dataType) throws IOException {
        new ColumnBuilder(name).setType(dataType).addToTable(table);
    }

    /**
     * 向表中添加列，以demoColumn为模板
     *
     * @param table
     * @param demoColumn
     * @throws IOException
     */
    public static void addColumnToTable(Table table, Column demoColumn) throws IOException {
        new ColumnBuilder(demoColumn.getName()).setFromColumn(demoColumn).addToTable(table);
    }

}
