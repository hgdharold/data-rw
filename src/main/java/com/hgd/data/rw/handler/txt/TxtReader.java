package com.hgd.data.rw.handler.txt;

import com.hgd.data.rw.handler.AbstractReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author hgd
 * @date 2020/4/22
 * <p>
 * TODO 在build中init方法
 */
public class TxtReader extends AbstractReader<String> {

    private LineIterator lineIterator;

    private TxtReader(File file) {
        super(file);
    }

    private void init() throws IOException {
        this.lineIterator = FileUtils.lineIterator(file);
    }

    @Override
    protected Closeable getOpenedResource() {
        return lineIterator;
    }

    @Override
    protected Iterator<String> genIterator() {
        return lineIterator;
    }

    public static class Builder {

        private File file;

        private Builder(File file) {
            this.file = file;
        }

        public TxtReader build() throws IOException {
            TxtReader txtReader = new TxtReader(file);
            txtReader.init();
            return txtReader;
        }
    }

    public static Builder builder(File file) {
        return new Builder(file);
    }

}
