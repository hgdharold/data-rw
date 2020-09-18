package com.hgd.data.rw.common;


import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 简单工具方法类
 *
 * @author hgd
 * @date 2020/4/26
 */

public class Helper {

    public static <K, V> Map<K, V> kvMap(K[] header, V[] row) {
        if (row == null || header == null || header.length == 0) {
            return null;
        }
        if (row.length != header.length) {
            row = Arrays.copyOf(row, header.length);
        }
        Map<K, V> res = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            K title = header[i];
            if (title != null) {
                res.put(title, row[i]);
            }
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> kvMap(List<K> header, List<V> row) {
        return kvMap((K[]) header.toArray(), (V[]) row.toArray());
    }

    public static <T> T indexOfIterator(Iterator<T> iterator, int index) {
        int count = 0;
        while (iterator.hasNext()) {
            if (count == index) {
                return iterator.next();
            }
            count++;
        }
        return null;
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }

    public static <T> void blockingQueuePut(BlockingQueue<T> queue, T item, T poison) throws InterruptedException {
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            try {
                queue.put(poison);
            } catch (InterruptedException e1) {
                throw e1;
            }
            throw e;
        }
    }

    public static <T> void blockingQueuePut(BlockingQueue<T> queue, T item, T poison, long waitMs) throws InterruptedException {
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            try {
                queue.offer(poison, waitMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e1) {
                throw e1;
            }
            throw e;
        }
    }

    public static String[] concat(String[]... arrList) {
        String[] result = new String[0];
        for (String[] arr : arrList) {
            if (arr != null) {
                int oldLength = result.length;
                result = Arrays.copyOf(result, oldLength + arr.length);
                System.arraycopy(arr, 0, result, oldLength, arr.length);
            }
        }
        return result;
    }

    public static String getRunningDir() throws URISyntaxException {
        URI uri = Helper.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return new File(uri).getParent();
    }

}