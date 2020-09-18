package com.hgd.data.rw.customized;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

/**
 * @author hgd
 * @date 2020/08/12
 */
public class BlockableIterator<T> implements Iterator<T> {

    private final BlockingQueue<T> dataBuffer;
    private T error;
    private T completed;
    private boolean hasNext = true;

    public BlockableIterator(BlockingQueue<T> dataBuffer, T error, T completed) {
        this.dataBuffer = dataBuffer;
        this.error = error;
        this.completed = completed;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public T next() {
        try {
            T data = dataBuffer.take();
            if (data == completed || data == error) {
                hasNext = false;
            }
            return data;
        } catch (InterruptedException e) {
            hasNext = false;
            return error;
        }
    }
}
