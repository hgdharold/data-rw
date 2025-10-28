package com.hgd.data.rw.west;

import com.hgd.data.rw.handler.excel.DefaultExcelReader;

import java.io.File;
import java.util.Arrays;

public class ReadExcelSyncTest {
    public static void main(String[] args) throws Exception {
        String filePath = "/Users/hgd/Downloads/work-temp-data/世界500强企业.xlsx";
        try (DefaultExcelReader reader = DefaultExcelReader
                .builder(new File(filePath))
//                .locale(Locale.ENGLISH)
                .build()) {
            int count = 0;
            for (String[] strings : reader) {
                if (count < 5) {
                    System.out.println(Arrays.toString(strings));
                }
                count++;
            }
            System.out.println(count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
