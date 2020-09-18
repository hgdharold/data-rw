import com.alibaba.fastjson.JSON;
import com.healthmarketscience.jackcess.Row;
import com.hgd.data.rw.common.ExcelUtil;
import com.hgd.data.rw.common.Helper;
import com.hgd.data.rw.customized.CustomXssfSheetXmlHandler.StyledRow;
import com.hgd.data.rw.handler.Reader;
import com.hgd.data.rw.handler.access.AccessReader;
import com.hgd.data.rw.handler.csv.CsvReader;
import com.hgd.data.rw.handler.excel.AbstractExcelReader;
import com.hgd.data.rw.handler.excel.DefaultExcelReader;
import com.hgd.data.rw.handler.excel.StyledExcelReader;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author hgd
 * @date 2020/4/26
 */

public class ReaderExample {

    @Test
    public void testCsv() {
        File file = new File("examples/sample/abc.csv");
        long start = System.currentTimeMillis();
        int count = 0;
        try (
                Reader<String[]> reader = CsvReader.builder(file)
//                        .separator('\t')
//                        .ignoreQuotations(false)
//                        .rfc4180(true)
                        .build()
        ) {
            Iterator<String[]> iterator = reader.iterator();
            String[] head = iterator.next();
            while (iterator.hasNext()) {
                count++;
                String[] row = iterator.next();
                System.out.println(Helper.kvMap(head, row));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(System.currentTimeMillis() - start);
        System.out.println(count);
    }


    @Test
    public void testAccess() {
//        File file = new File("examples/sample/opq.accdb");
        File file = new File("/Users/hgd/Downloads/baidunetdiskdownload/EXP_INDIA-201902-1.accdb");

        long start = System.currentTimeMillis();
        int count = 0;
        try (
                Reader<Row> reader = AccessReader.builder(file).build()
        ) {
            Iterator<Row> iterator = reader.iterator();
            int limit = 0;
            while (iterator.hasNext() && (limit == 0 || count < limit)) {
                count++;
                Row row = iterator.next();
                if (row != null) {
                    System.out.println(JSON.toJSONString(row));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(System.currentTimeMillis() - start);
        System.out.println(count);
    }

    @Test
    public void testExcelSynchronize() {
        File file = new File("examples/sample/def.xlsx");
        long start = System.currentTimeMillis();
        int count = 0;

        try (
                Reader<String[]> reader = DefaultExcelReader.builder(file).build()
        ) {
            Iterator<String[]> iterator = reader.iterator();
            if (iterator != null) { // 解析异常判断
                while (iterator.hasNext()) {
                    String[] row = iterator.next();
                    count++;
                    System.out.println("Row:" + Arrays.toString(row));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(System.currentTimeMillis() - start);
        System.out.println(count);
    }

    @Test
    public void testExcelAsync() {
        File file = new File("examples/sample/def.xlsx");
        long start = System.currentTimeMillis();
        int count = 0;

        final BlockingQueue<String[]> queue = new ArrayBlockingQueue<>(3);
        final String[] queuePoison = new String[0];
        final String[] queueCompleted = new String[0];
        try (
                AbstractExcelReader<String[]> reader = DefaultExcelReader.builder(file).build()
        ) {
            reader.setRowConsumer((row, idx) -> {
                // TODO 自定义consumer抛出异常
                try {
                    Helper.blockingQueuePut(queue, row, queuePoison, 5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            reader.setEndCall(() -> {
                try {
                    Helper.blockingQueuePut(queue, queueCompleted, queuePoison, 5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            // parse in a separate thread
            Thread readThread = new Thread(() -> {
                try {
                    reader.parse();
                } catch (IOException | SAXException e) {
                    e.printStackTrace();
                    try {
                        queue.offer(queuePoison, 5000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            });
            readThread.start();
            Thread.sleep(1000);
            readThread.interrupt();
            while (true) {
                String[] row = queue.take();
                if (row == queuePoison) {
                    System.out.println("error happened");
                    break;
                } else if (row != queueCompleted) {
                    count++;
                    System.out.println(Arrays.toString(row));
                } else {
                    System.out.println("completed");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(System.currentTimeMillis() - start);
        System.out.println(count);
    }

    @Test
    public void testStyledExcel() {
        File oldXlsx = new File("examples/sample/test.xlsx");
        File newXlsx = new File("examples/sample/test-copy.xlsx");
        if (!newXlsx.exists()) {
            try {
                newXlsx.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (
                StyledExcelReader excelReader = StyledExcelReader.builder(oldXlsx).build();
                XSSFWorkbook workbook = new XSSFWorkbook();
                FileOutputStream outputStream = new FileOutputStream(newXlsx)
        ) {
            Map<Short, Short> indexMap = ExcelUtil.cloneStylesTable(workbook, excelReader.getStylesTable());
            Sheet sheet = workbook.createSheet(excelReader.getSheetName());
            Iterator<StyledRow> iterator = excelReader.iterator();
            while (iterator.hasNext()) {
                StyledRow row = iterator.next();
                if (row != null) {
                    ExcelUtil.addRowWithStyle(indexMap, sheet, row);
                }
            }
            ExcelUtil.addSheetStyle(indexMap, sheet, excelReader.getSheetStyle(), true);
            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
