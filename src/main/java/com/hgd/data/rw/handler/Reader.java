package com.hgd.data.rw.handler;

import java.io.Closeable;

/**
 * @author hgd
 * @date 2020/4/22
 */
public interface Reader<T> extends Closeable, Iterable<T> {

}
