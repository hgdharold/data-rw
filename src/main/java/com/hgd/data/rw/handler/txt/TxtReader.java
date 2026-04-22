package com.hgd.data.rw.handler.txt;

import com.hgd.data.rw.common.CharsetDetector;
import com.hgd.data.rw.handler.AbstractReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static com.hgd.data.rw.common.CharsetDetector.detectEncoding;

/**
 * @author hgd
 * @date 2020/4/22
 * <p>
 * TODO 在build中init方法
 */
public class TxtReader extends AbstractReader<String> {

    private static final Logger log = LoggerFactory.getLogger(TxtReader.class);

    private LineIterator lineIterator;

    private TxtReader(File file) {
        super(file);
    }

    private void init() throws IOException {
        CharsetDetector.CharsetDetectionResult detectedEncoding = detectEncoding(file);
        if (!"UTF-8".equals(detectedEncoding.charset)) {
            log.warn("!!! detected file encoding is not UTF-8: {}", detectedEncoding.charset);
        }
        this.lineIterator = FileUtils.lineIterator(file, detectedEncoding.charset);
    }

    @Override
    protected Iterator<String> genIterator() {
        return lineIterator;
    }

    @Override
    public void close() throws IOException {
        if (lineIterator != null) {
            lineIterator.close();
        }
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
