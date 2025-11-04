package com.hgd.data.rw.common;

import java.util.*;
import java.util.regex.Pattern;

public class StringHelper {

    /**
     * 全面的Unicode空白字符列表
     * 包含所有重要的Unicode空白字符，基于Unicode标准和实际使用频率
     */
    private static final List<String> COMPREHENSIVE_WHITESPACE_LIST = Arrays.asList(
            // ===== 基本ASCII空白字符 =====
            "\\u0020",                // SPACE - 空格

            // ===== C0控制字符范围 =====
            "\\u0009-\\u000D",        // 标准空白
            // U+0009: 制表符
            // U+000A: 换行符
            // U+000B: 垂直制表符 ✓（但注释未提及）
            // U+000C: 换页符 ✓（但注释未提及）
            // U+000D: 回车符
            "\\u001C-\\u001F",        // 信息分隔符
            "\\u0019",                // 介质结束符
            "\\u001A",                // 替换字符

            // ===== C1控制字符中的空白 =====
            "\\u0085",                // NEXT LINE - 下一行

            // ===== 拉丁语补充中的空白 =====
            "\\u00A0",                // NO-BREAK SPACE - 不间断空格

            // ===== 欧甘文字 =====
            "\\u1680",                // OGHAM SPACE MARK - 欧甘空格标记

            // ===== 通用标点符号中的各种空格 =====
            "\\u2000-\\u200A",        // 各种宽度空格:
            // \\u2000: EN QUAD
            // \\u2001: EM QUAD
            // \\u2002: EN SPACE
            // \\u2003: EM SPACE
            // \\u2004: THREE-PER-EM SPACE
            // \\u2005: FOUR-PER-EM SPACE
            // \\u2006: SIX-PER-EM SPACE
            // \\u2007: FIGURE SPACE
            // \\u2008: PUNCTUATION SPACE
            // \\u2009: THIN SPACE
            // \\u200A: HAIR SPACE

            // ===== 零宽字符 =====
            "\\u200B",                // ZERO WIDTH SPACE - 零宽空格
            "\\u200C",                // ZERO WIDTH NON-JOINER - 零宽非连接符
            "\\u200D",                // ZERO WIDTH JOINER - 零宽连接符
            "\\u2060",                // WORD JOINER - 单词连接符
            "\\uFEFF",                // ZERO WIDTH NO-BREAK SPACE - 零宽不间断空格(BOM)

            // ===== 行和段落分隔符 =====
            "\\u2028",                // LINE SEPARATOR - 行分隔符
            "\\u2029",                // PARAGRAPH SEPARATOR - 段落分隔符

            // ===== 数学运算符中的空白 =====
            "\\u205F",                // MEDIUM MATHEMATICAL SPACE - 中等数学空格

            // ===== 标点符号中的特殊空格 =====
            "\\u202F",                // NARROW NO-BREAK SPACE - 窄不间断空格

            // ===== CJK标点符号（中文、日文、韩文） =====
            "\\u3000",                // IDEOGRAPHIC SPACE - 表意文字空格（全角空格）

            // ===== 蒙古文 =====
            "\\u180E",                // MONGOLIAN VOWEL SEPARATOR - 蒙古文元音分隔符

            // ===== 其他可能被视为空白的字符 =====
            "\\u200E",                // LEFT-TO-RIGHT MARK - 从左到右标记
            "\\u200F",                // RIGHT-TO-LEFT MARK - 从右到左标记
            "\\u2061",                // FUNCTION APPLICATION - 函数应用
            "\\u2062",                // INVISIBLE TIMES - 不可见乘号
            "\\u2063",                // INVISIBLE SEPARATOR - 不可见分隔符

            // ===== 罕见但可能遇到的空白字符 =====
            "\\u1361",                // ETHIOPIC WORDSPACE - 埃塞俄比亚单词空格
            "\\u10100",               // AEGEAN WORD SEPARATOR LINE - 爱琴海单词分隔线
            "\\u10101",               // AEGEAN WORD SEPARATOR DOT - 爱琴海单词分隔点
            "\\u1039F",               // UGARITIC WORD DIVIDER - 乌加里特单词分隔符
            "\\u1091F"                // PHOENICIAN WORD SEPARATOR - 腓尼基单词分隔符
    );

    private static final Set<Character> COMPREHENSIVE_WHITESPACE_SET = createWhitespaceSet();

    private static final String WHITESPACE_CHARS = String.join("", COMPREHENSIVE_WHITESPACE_LIST);

    // 预编译正则表达式模式
    private static final Pattern COMPLETE_STRIP_PATTERN =
            Pattern.compile("^[" + WHITESPACE_CHARS + "]+|[" + WHITESPACE_CHARS + "]+$");

    private static final Pattern LEADING_WHITESPACE_PATTERN =
            Pattern.compile("^[" + WHITESPACE_CHARS + "]+");

    private static final Pattern TRAILING_WHITESPACE_PATTERN =
            Pattern.compile("[" + WHITESPACE_CHARS + "]+$");

    private static final Pattern ANY_WHITESPACE_PATTERN =
            Pattern.compile("[" + WHITESPACE_CHARS + "]");

    private static final Pattern MULTIPLE_WHITESPACE_PATTERN =
            Pattern.compile("[" + WHITESPACE_CHARS + "]+");

    /**
     * 根据空白字符列表创建对应的字符Set
     */
    private static Set<Character> createWhitespaceSet() {
        Set<Character> set = new HashSet<>(200);

        // 1. 添加标准空白字符 (\s 对应的字符)
        set.add(' ');        // 空格 U+0020
        set.add('\t');       // 制表符 U+0009
        set.add('\n');       // 换行符 U+000A
        set.add('\u000B');   // 垂直制表符 U+000B
        set.add('\f');       // 换页符 U+000C
        set.add('\r');       // 回车符 U+000D

        // 2. 处理列表中的每个字符定义（跳过第一个\\s）
        for (int i = 1; i < COMPREHENSIVE_WHITESPACE_LIST.size(); i++) {
            String charDef = COMPREHENSIVE_WHITESPACE_LIST.get(i);

            if (charDef.contains("-")) {
                // 处理字符范围，如 "\\u2000-\\u200A"
                try {
                    String[] parts = charDef.split("-");
                    if (parts.length == 2) {
                        char startChar = (char) Integer.parseInt(parts[0].substring(2), 16);
                        char endChar = (char) Integer.parseInt(parts[1].substring(2), 16);
                        // 添加字符范围
                        for (char ch = startChar; ch <= endChar; ch++) {
                            set.add(ch);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("无法处理字符范围: " + charDef);
                }
            } else {
                // 处理单个字符，如 "\\u0085"
                try {
                    char ch = (char) Integer.parseInt(charDef.substring(2), 16);
                    set.add(ch);
                } catch (Exception e) {
                    System.err.println("无法处理字符定义: " + charDef);
                }
            }
        }

        return Collections.unmodifiableSet(set);
    }

    /**
     * 去除字符串两端的Unicode空白字符，
     * 原生trim方法不能全部去除Unicode空白字符
     * 类似于JDK11+的strip()方法
     */
    public static String customStrip(String str) {
        if (str == null || str.isEmpty()) return str;
        int length = str.length();
        int start = 0;
        int end = length;

        // 遍历开头空白字符
        while (start < end && COMPREHENSIVE_WHITESPACE_SET.contains(str.charAt(start))) {
            start++;
        }

        // 遍历结尾空白字符
        while (end > start && COMPREHENSIVE_WHITESPACE_SET.contains(str.charAt(end - 1))) {
            end--;
        }

        return start > 0 || end < length ? str.substring(start, end) : str;
    }

}
