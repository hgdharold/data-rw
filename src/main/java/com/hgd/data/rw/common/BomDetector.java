package com.hgd.data.rw.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * BOM检测工具类
 * <p>
 * 检测完成后，可以让流跳过BOM：
 * FileInputStream fis = new FileInputStream(file);
 * if (bomResult.hasBom()) {
 * fis.skip(bomResult.bomLength);
 * }
 */
public class BomDetector {

    /**
     * 所有常见BOM类型定义
     */
    public enum BomType {
        UTF_8("UTF-8", new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}),
        UTF_16_BE("UTF-16BE", new byte[]{(byte) 0xFE, (byte) 0xFF}),
        UTF_16_LE("UTF-16LE", new byte[]{(byte) 0xFF, (byte) 0xFE}),
        UTF_32_BE("UTF-32BE", new byte[]{0x00, 0x00, (byte) 0xFE, (byte) 0xFF}),
        UTF_32_LE("UTF-32LE", new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00, 0x00}),
        SCSU("SCSU", new byte[]{(byte) 0x0E, (byte) 0xFE, (byte) 0xFF}),
        BOCU_1("BOCU-1", new byte[]{(byte) 0xFB, (byte) 0xEE, (byte) 0x28}),
        GB_18030("GB18030", new byte[]{(byte) 0x84, (byte) 0x31, (byte) 0x95, (byte) 0x33});

        public final String charset;
        public final byte[] signature;
        public final int length;

        BomType(String charset, byte[] signature) {
            this.charset = charset;
            this.signature = signature;
            this.length = signature.length;
        }
    }

    /**
     * 检测文件的BOM类型
     */
    public static BomDetectionResult detectBom(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return detectBom(fis);
        }
    }

    /**
     * 检测输入流的BOM类型
     */
    public static BomDetectionResult detectBom(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4]; // 最大BOM长度为4字节
        int bytesRead = inputStream.read(buffer);

        for (BomType bomType : BomType.values()) {
            if (bytesRead >= bomType.length &&
                    matchesBom(buffer, bomType.signature, bomType.length)) {
                return new BomDetectionResult(
                        Charset.forName(bomType.charset),
                        bomType.name(),
                        bomType.length
                );
            }
        }

        return new BomDetectionResult(StandardCharsets.UTF_8, "UNKNOWN", 0);
    }

    /**
     * 检查字节是否匹配BOM签名
     */
    private static boolean matchesBom(byte[] buffer, byte[] bomSignature, int length) {
        for (int i = 0; i < length; i++) {
            if (buffer[i] != bomSignature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * BOM检测结果
     */
    public static class BomDetectionResult {
        public final Charset charset;
        public final String bomType;
        public final int bomLength;

        public BomDetectionResult(Charset charset, String bomType, int bomLength) {
            this.charset = charset;
            this.bomType = bomType;
            this.bomLength = bomLength;
        }

        public boolean hasBom() {
            return bomLength > 0;
        }

        @Override
        public String toString() {
            return String.format("编码: %s, BOM类型: %s, BOM长度: %d字节, 有BOM: %s",
                    charset.name(), bomType, bomLength, hasBom());
        }
    }
}