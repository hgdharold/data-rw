package com.hgd.data.rw.common;

import com.ibm.icu.text.CharsetMatch;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CharsetDetector {
    private static final Logger log = LoggerFactory.getLogger(CharsetDetector.class);

    // 配置常量
    private static final int SAMPLE_SIZE = 5 * 1024 * 1024;        // 采样5MB
    private static final int STREAMING_LIMIT = 5 * 1024 * 1024;   // 流式检测上限5MB
    private static final int CONFIDENCE_THRESHOLD = 80;            // 置信度阈值80%

    /**
     * 主入口：按照新逻辑的编码检测
     * 逻辑：1.BOM检测 → 2.采样检测 → 3.低置信度则流式检测
     */
    public static CharsetDetectionResult detectEncoding(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + file.getPath());
        }

        long fileSize = file.length();
        log.trace("文件大小: {} 字节", String.format("%,d", fileSize));
        // 1. BOM检测（最高优先级）
        CharsetDetectionResult bomResult = detectBOM(file);
        if (bomResult != null) {
            log.trace("✓ BOM检测成功: {}", bomResult.charset);
            return bomResult;
        }
        // 2. 采样检测（读取前Min(1MB, fileSize)）
        int sampleSize = (int) Math.min(SAMPLE_SIZE, fileSize);
        CharsetDetectionResult sampleResult = detectWithSampling(file, sampleSize);

        // 3. 判断置信度，决定是否进行流式检测
        if (sampleResult.confidence >= CONFIDENCE_THRESHOLD) {
            log.trace("✓ 采样检测结果置信度≥80%：{}", sampleResult);
            return sampleResult;
        }
        // 4. 流式检测
        CharsetDetectionResult streamingResult = detectWithStreaming(file, STREAMING_LIMIT);
        if (streamingResult.confidence > 0) {
            log.trace("✓ 流式检测成功：{}", streamingResult);
            return streamingResult;
        }
        // 5. 所有检测都失败，返回默认值
        log.trace("✗ 所有检测方法均失败，返回默认编码UTF-8");
        return new CharsetDetectionResult("UTF-8", 0, null, "默认回退");
    }

    /**
     * BOM检测
     */
    private static CharsetDetectionResult detectBOM(File file) throws IOException {
        byte[] bomSample = readFileSample(file, 4);

        if (bomSample.length >= 4) {
            // UTF-32 BE
            if (bomSample[0] == 0x00 && bomSample[1] == 0x00 &&
                    bomSample[2] == (byte) 0xFE && bomSample[3] == (byte) 0xFF) {
                return new CharsetDetectionResult("UTF-32BE", 100, null, "BOM");
            }
            // UTF-32 LE
            if (bomSample[0] == (byte) 0xFF && bomSample[1] == (byte) 0xFE &&
                    bomSample[2] == 0x00 && bomSample[3] == 0x00) {
                return new CharsetDetectionResult("UTF-32LE", 100, null, "BOM");
            }
        }

        if (bomSample.length >= 3) {
            // UTF-8 BOM
            if (bomSample[0] == (byte) 0xEF && bomSample[1] == (byte) 0xBB && bomSample[2] == (byte) 0xBF) {
                return new CharsetDetectionResult("UTF-8", 100, null, "BOM");
            }
        }

        if (bomSample.length >= 2) {
            // UTF-16 BE
            if (bomSample[0] == (byte) 0xFE && bomSample[1] == (byte) 0xFF) {
                return new CharsetDetectionResult("UTF-16BE", 100, null, "BOM");
            }
            // UTF-16 LE
            if (bomSample[0] == (byte) 0xFF && bomSample[1] == (byte) 0xFE) {
                return new CharsetDetectionResult("UTF-16LE", 100, null, "BOM");
            }
        }

        return null;
    }

    /**
     * 采样检测
     */
    private static CharsetDetectionResult detectWithSampling(File file, int sampleSize) throws IOException {
        if (sampleSize <= 0) {
            return new CharsetDetectionResult("UTF-8", 0, null, "采样失败");
        }

        byte[] sampleData = readFileSample(file, sampleSize);

        // 使用ICU检测器
        try {
            com.ibm.icu.text.CharsetDetector icuDetector = new com.ibm.icu.text.CharsetDetector();
            icuDetector.setText(sampleData);
            CharsetMatch[] matches = icuDetector.detectAll();

            if (matches.length > 0) {
                CharsetMatch bestMatch = matches[0];
                String method = sampleSize >= file.length() ? "完整采样" : "部分采样";
                return new CharsetDetectionResult(
                        bestMatch.getName(),
                        bestMatch.getConfidence(),
                        bestMatch.getLanguage(),
                        method
                );
            }
        } catch (Exception e) {
            System.err.println("ICU采样检测失败: " + e.getMessage());
        }

        return new CharsetDetectionResult("UTF-8", 0, null, "采样检测失败");
    }

    /**
     * 流式检测（带上限限制）
     */
    private static CharsetDetectionResult detectWithStreaming(File file, int limitBytes) throws IOException {
        UniversalDetector detector = new UniversalDetector(null);
        int totalBytesRead = 0;
        boolean detectionCompleted = false;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                detector.handleData(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // 检测器提前完成
                if (detector.isDone()) {
                    detectionCompleted = true;
                    break;
                }

                // 达到流式检测上限
                if (totalBytesRead >= limitBytes) {
                    break;
                }
            }
        }

        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();

        if (encoding != null) {
            int confidence = detectionCompleted ? 90 : 70;
            return new CharsetDetectionResult(encoding, confidence, null, "流式检测");
        }

        // 流式检测失败
        return new CharsetDetectionResult("UTF-8", 0, null, "流式检测失败");
    }

    /**
     * 读取文件样本
     */
    private static byte[] readFileSample(File file, int sampleSize) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            int actualSampleSize = (int) Math.min(sampleSize, file.length());
            byte[] sample = new byte[actualSampleSize];
            int bytesRead = fis.read(sample);

            if (bytesRead < sample.length) {
                byte[] actualSample = new byte[bytesRead];
                System.arraycopy(sample, 0, actualSample, 0, bytesRead);
                return actualSample;
            }
            return sample;
        }
    }

    /**
     * 检测结果类
     */
    public static class CharsetDetectionResult {
        public final String charset;
        public final int confidence;
        public final String language;
        public final String detector;

        public CharsetDetectionResult(String charset, int confidence, String language, String detector) {
            this.charset = charset;
            this.confidence = confidence;
            this.language = language;
            this.detector = detector;
        }

        @Override
        public String toString() {
            return String.format("编码: %s, 置信度: %d%%, 方法: %s",
                    charset, confidence, detector);
        }

        public boolean isConfident() {
            return confidence >= CONFIDENCE_THRESHOLD;
        }
    }

}