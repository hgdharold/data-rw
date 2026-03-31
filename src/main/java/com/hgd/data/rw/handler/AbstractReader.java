package com.hgd.data.rw.handler;

import java.io.File;
import java.util.Iterator;

/**
 * @author hgd
 * @date 2020/04/22
 */
public abstract class AbstractReader<T> implements Reader<T> {
    protected File file;
    protected Iterator<T> iterator;

    protected AbstractReader(File file) {
        this.file = file;
    }

    @Override
    public Iterator<T> iterator() {
        if (iterator == null) {
            iterator = genIterator();
        }
        return iterator;
    }

    /**
     * 生成遍历器
     */
    protected abstract Iterator<T> genIterator();

}
