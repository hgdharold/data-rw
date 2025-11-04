package com.hgd.data.rw.west;

import com.healthmarketscience.jackcess.Row;
import com.hgd.data.rw.handler.access.AccessReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static com.hgd.data.rw.common.StringHelper.customStrip;

public class ReadMdbTest {
    public static void main(String[] args) {
        String filePath = "/Users/hgd/Downloads/work-temp-data/海外统计数据/IMP_ENGLAND_202406.mdb";
        try (AccessReader accessReader = AccessReader
                .builder(new File(filePath))
                .build()) {
            System.out.println(accessReader.getTableNames());
            System.out.println(accessReader.getTableName());
            Map<String, String> columnNameTypeMap = accessReader.getColumnNameTypeMap();
            System.out.println(columnNameTypeMap);
            System.out.println(columnNameTypeMap.size());
            Map.Entry<String, String> entry = new ArrayList<>(columnNameTypeMap.entrySet()).get(0);
            String key = entry.getKey();
            String keyTrim = customStrip(key);
            System.out.println(key.equals(keyTrim));
            Iterator<Row> rowIterator = accessReader.iterator();
            int count = 0;
            while (rowIterator.hasNext()) {
                Row next = rowIterator.next();
                if (next != null) {
                    if (count < 3) {
                        System.out.println(next);
                    }
                    count++;
                }
            }
            System.out.println(count);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
