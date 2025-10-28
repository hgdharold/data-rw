package com.hgd.data.rw.handler.excel.customized;

import com.hgd.data.rw.handler.excel.customized.StyledEleDef.SheetStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.Comments;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;

import static org.apache.poi.xssf.usermodel.XSSFRelation.NS_SPREADSHEETML;

/**
 * 采用组合模式替代继承，规避父类私有变量访问限制
 */
@Slf4j
public class CustomXssfSheetXmlHandler extends DefaultHandler {

    /**
     * 持有原生 XSSFSheetXMLHandler 实例，复用其基础解析逻辑
     */
    private final XSSFSheetXMLHandler delegate;
    /**
     * 自定义的回调接口（扩展样式支持）{@link XSSFSheetXMLHandler.SheetContentsHandler}
     */
    private final CustomSheetContentsHandler customOutput;
    /**
     * 自定义的sheet的整体样式，如列宽度、列样式、合并单元格等
     */
    private final SheetStyle sheetStyle;

    // 当前解析上下文（行样式、单元格样式等）
    private XSSFCellStyle currentRowStyle;
    private Short currentRowHeight;
    private XSSFCellStyle currentCellStyle;
    private CellType currentCellDataType; // 当前单元格的数据类型（对应原类的 nextDataType）

    // 原生父类的依赖参数（需传递给 delegate）
    private final Styles stylesTable;
    private final Comments comments;
    private final SharedStrings sharedStrings;
    private final DataFormatter formatter;
    private final boolean formulasNotResults;

    /**
     * 构造方法：初始化组合的 delegate 和自定义样式数据
     */
    public CustomXssfSheetXmlHandler(Styles styles, Comments comments, SharedStrings strings,
                                     CustomSheetContentsHandler output, DataFormatter dataFormatter,
                                     boolean formulasNotResults) {
        this.stylesTable = styles;
        this.comments = comments;
        this.sharedStrings = strings;
        this.formatter = dataFormatter;
        this.formulasNotResults = formulasNotResults;
        this.customOutput = output;

        // 初始化原生解析器（delegate），使用包装后的回调接口拦截结果
        this.delegate = new XSSFSheetXMLHandler(
                styles, comments, strings,
                new WrappedSheetContentsHandler(), // 拦截父类的回调
                dataFormatter, formulasNotResults
        );

        // 初始化样式数据
        this.sheetStyle = new SheetStyle()
                .setMergedRegions(new ArrayList<>())
                .setColumnWidth(new HashMap<>())
                .setColumnStyle(new HashMap<>());
    }

    /**
     * 重写 startElement：补充一些操作及样式解析
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (uri == null || !uri.equals(NS_SPREADSHEETML)) {
            return;
        }
        // 工作表开始
        else if ("worksheet".equals(localName)) {
            customOutput.startSheet();
        }
        // 解析工作表默认样式
        else if ("sheetFormatPr".equals(localName)) {
            String defaultRowHeight = attributes.getValue("defaultRowHeight");
            if (defaultRowHeight != null) {
                sheetStyle.setDefaultRowHeight(Float.valueOf(defaultRowHeight));
            }
            String defaultColWidth = attributes.getValue("defaultColWidth");
            if (defaultColWidth != null) {
                sheetStyle.setDefaultColumnWidth(Integer.valueOf(defaultColWidth));
            }
        }
        // 解析列的整体设置：样式和宽度
        else if ("col".equals(localName)) {
            int minCol = Integer.parseInt(attributes.getValue("min")) - 1; // 转为0-based
            int maxCol = Integer.parseInt(attributes.getValue("max")) - 1;
            String width = attributes.getValue("width");
            String styleIdx = attributes.getValue("s");

            XSSFCellStyle colStyle = getCellStyle(styleIdx);
            for (int col = minCol; col <= maxCol; col++) {
                if (width != null) {
                    sheetStyle.getColumnWidth().put(col, Float.valueOf(width));
                }
                if (colStyle != null) {
                    sheetStyle.getColumnStyle().put(col, colStyle);
                }
            }
        }
        // 工作表数据区域开始（sheetData）
        else if ("sheetData".equals(localName)) {
            customOutput.startSheetData();
        }
        // 解析行样式和高度（row）
        else if ("row".equals(localName)) {
            // 解析行样式和高度
            String styleIdx = attributes.getValue("s");
            currentRowStyle = getCellStyle(styleIdx);
            String height = attributes.getValue("ht");
            currentRowHeight = height != null ? Short.valueOf(height) : null;
        }
        // 解析单元格样式（c）
        else if ("c".equals(localName)) {
            // 解析单元格样式
            String styleIdx = attributes.getValue("s");
            currentCellStyle = getCellStyle(styleIdx);

            // 记录原始类型（t 属性）：如 "b"（布尔）、"s"（共享字符串）、"n"（数字，默认）、"e"（错误）等
            String rawDataType = attributes.getValue("t");
            if ("b".equals(rawDataType))
                currentCellDataType = CellType.BOOLEAN;
            else if ("e".equals(rawDataType))
                currentCellDataType = CellType.ERROR;
            else if ("inlineStr".equals(rawDataType))
                currentCellDataType = CellType.STRING;
            else if ("s".equals(rawDataType))
                currentCellDataType = CellType.STRING;
            else if ("str".equals(rawDataType))
                currentCellDataType = CellType.STRING;
            else {
                currentCellDataType = CellType.NUMERIC;
                if (currentCellStyle == null && stylesTable.getNumCellStyles() > 0) {
                    currentCellStyle = stylesTable.getStyleAt(0);
                }
            }
        }
        // 解析公式（f 标签）：标记当前单元格为公式类型
        else if ("f".equals(localName)) {
            currentCellDataType = CellType.FORMULA;
        }
        // 解析合并单元格（mergeCell）
        else if ("mergeCell".equals(localName)) {
            String ref = attributes.getValue("ref");
            if (ref != null) {
                sheetStyle.getMergedRegions().add(CellRangeAddress.valueOf(ref));
            }
        }
        // 调用原生解析器处理基础逻辑（如单元格值、行列号解析）
        delegate.startElement(uri, localName, qName, attributes);
    }

    /**
     * 重写 endElement：先调用原生解析器，再补充样式相关回调
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (uri == null || !uri.equals(NS_SPREADSHEETML)) {
            return;
        }
        // 整个工作表结束
        else if ("worksheet".equals(localName)) {
            customOutput.endSheet(sheetStyle);
        }
        // 调用原生解析器处理基础逻辑
        delegate.endElement(uri, localName, qName);
    }

    /**
     * 重写 characters：直接委托给原生解析器（复用其值缓存逻辑）
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        delegate.characters(ch, start, length);
    }

    // 工具方法：通过样式索引获取样式（使用自己持有的 stylesTable）
    private XSSFCellStyle getCellStyle(String styleIdx) {
        if (styleIdx == null || stylesTable == null) {
            return null;
        }
        try {
            int idx = Integer.parseInt(styleIdx);
            return stylesTable.getStyleAt(idx);
        } catch (NumberFormatException e) {
            log.error("Invalid style index: {}", styleIdx, e);
            return null;
        }
    }

    /**
     * 包装原生的 SheetContentsHandler，拦截基础回调并附加样式信息
     */
    private class WrappedSheetContentsHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        @Override
        public void startRow(int rowNum) {
            customOutput.startRow(rowNum, currentRowStyle, currentRowHeight);
        }

        @Override
        public void endRow(int rowNum) {
            customOutput.endRow(rowNum, currentRowStyle, currentRowHeight);
        }

        @Override
        public void cell(String cellRef, String formattedValue, XSSFComment comment) {
            // 传递给自定义回调（包含样式和类型）
            customOutput.cell(cellRef, formattedValue, comment, currentCellStyle, currentCellDataType);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            customOutput.headerFooter(text, isHeader, tagName);
        }

        @Override
        public void endSheet() {
            customOutput.endSheetData();
        }
    }

    public interface CustomSheetContentsHandler {

        void startRow(int rowNum, CellStyle rowStyle, Short height);

        default void endRow(int rowNum, CellStyle rowStyle, Short height) {
        }

        void cell(String cellRef, String formattedValue, XSSFComment comment, XSSFCellStyle cellStyle, CellType cellType);

        default void headerFooter(String text, boolean isHeader, String tagName) {
        }

        default void startSheetData() {
        }

        default void endSheetData() {
        }

        default void startSheet() {
        }

        default void endSheet(SheetStyle sheetStyle) {
        }

    }

}
