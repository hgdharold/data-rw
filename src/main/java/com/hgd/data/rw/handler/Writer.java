package com.hgd.data.rw.handler;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * @author hgd
 * @date 2020/7/10
 */
public interface Writer<T> extends Closeable {

    /**
     * add one
     *
     * @param item
     * @throws IOException
     */
    void write(T item) throws IOException;

    /**
     * add many
     *
     * @param items
     * @throws IOException
     */
    void write(Collection<T> items) throws IOException;

    /**
     * set if flush or not after batch write operation
     *
     * @throws IOException
     */
    void flush() throws IOException;
}
