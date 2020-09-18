package com.hgd.data.rw.customized;


import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.Comments;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

import static org.apache.poi.xssf.usermodel.XSSFRelation.NS_SPREADSHEETML;

/**
 * 在原{@link XSSFSheetXMLHandler}的基础上，将各种样式加入回调函数中
 * <p>
 * 可用于带样式的复制，目前存在以下问题：
 * 样式可能不全
 * 颜色复制不准确
 *
 * @author hgd
 */
public class CustomXssfSheetXmlHandler extends DefaultHandler {
    private static final POILogger logger = POILogFactory.getLogger(CustomXssfSheetXmlHandler.class);

    /**
     * These are the different kinds of cells we support.
     * We keep track of the current one between
     * the parse and end.
     */
    enum xssfDataType {
        BOOLEAN,
        ERROR,
        FORMULA,
        INLINE_STRING,
        SST_STRING,
        NUMBER,
    }

    /**
     * Table with the styles used for formatting
     */
    private Styles stylesTable;

    /**
     * Table with cell comments
     */
    private Comments comments;

    /**
     * Read only access to the shared strings table, for looking
     * up (most) string cell's contents
     */
    private SharedStrings sharedStringsTable;

    /**
     * Where our text is going
     */
    private final SheetContentsHandler output;

    // Set when V parse element is seen
    private boolean vIsOpen;
    // Set when F parse element is seen
    private boolean fIsOpen;
    // Set when an Inline String "is" is seen
    private boolean isIsOpen;
    // Set when a header/footer element is seen
    private boolean hfIsOpen;

    // Set when cell parse element is seen;
    // used when cell close element is seen.
    private xssfDataType nextDataType;

    // Used to format numeric cell values.
    private short formatIndex;
    private String formatString;
    private final DataFormatter formatter;
    private int rowNum;
    // some sheets do not have rowNums, Excel can read them so we should try to handle them correctly as well
    private int nextRowNum;
    private String cellRef;
    private boolean formulasNotResults;

    // Gathers characters as they are seen.
    private StringBuilder value = new StringBuilder(64);
    private StringBuilder formula = new StringBuilder(64);
    private StringBuilder headerFooter = new StringBuilder(64);

    private Queue<CellAddress> commentCellRefs;

    private SheetStyle sheetStyle;
    // Set when cell parse element is seen;
    // used when cell close element is seen.
    private XSSFCellStyle nextCellStyle;

    /**
     * Accepts objects needed while parsing.
     *
     * @param styles  Table of styles
     * @param strings Table of shared strings
     */
    public CustomXssfSheetXmlHandler(
            Styles styles,
            Comments comments,
            SharedStrings strings,
            SheetContentsHandler sheetContentsHandler,
            DataFormatter dataFormatter,
            boolean formulasNotResults) {
        this.stylesTable = styles;
        this.comments = comments;
        this.sharedStringsTable = strings;
        this.output = sheetContentsHandler;
        this.formulasNotResults = formulasNotResults;
        this.nextDataType = xssfDataType.NUMBER;
        this.formatter = dataFormatter;
        init(comments);
    }

    /**
     * Accepts objects needed while parsing.
     *
     * @param styles  Table of styles
     * @param strings Table of shared strings
     */
    public CustomXssfSheetXmlHandler(
            Styles styles,
            SharedStrings strings,
            SheetContentsHandler sheetContentsHandler,
            DataFormatter dataFormatter,
            boolean formulasNotResults) {
        this(styles, null, strings, sheetContentsHandler, dataFormatter, formulasNotResults);
    }

    /**
     * Accepts objects needed while parsing.
     *
     * @param styles  Table of styles
     * @param strings Table of shared strings
     */
    public CustomXssfSheetXmlHandler(
            Styles styles,
            SharedStrings strings,
            SheetContentsHandler sheetContentsHandler,
            boolean formulasNotResults) {
        this(styles, strings, sheetContentsHandler, new DataFormatter(), formulasNotResults);
    }

    private void init(Comments commentsTable) {
        if (commentsTable != null) {
            commentCellRefs = new LinkedList<>();
            for (Iterator<CellAddress> iter = commentsTable.getCellAddresses(); iter.hasNext(); ) {
                commentCellRefs.add(iter.next());
            }
        }
        sheetStyle = new SheetStyle()
                .setMergedRegions(new ArrayList<>())
                .setColumnWidth(new HashMap<>())
                .setColumnStyle(new HashMap<>());
    }

    private boolean isTextTag(String name) {
        if ("v".equals(name)) {
            // Easy, normal v text tag
            return true;
        }
        if ("inlineStr".equals(name)) {
            // Easy inline string
            return true;
        }
        if ("t".equals(name) && isIsOpen) {
            // Inline string <is><t>...</t></is> pair
            return true;
        }
        // It isn't a text tag
        return false;
    }

    @Override
    @SuppressWarnings("unused")
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

        if (uri != null && !uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        // "sheetFormatPr" attr: defaultRowHeight defaultColWidth
        if ("sheetFormatPr".equals(localName)) {
            String defaultRowHeightStr = attributes.getValue("defaultRowHeight");
            if (defaultRowHeightStr != null) {
                sheetStyle.setDefaultRowHeight(Float.valueOf(defaultRowHeightStr));
            }
            String defaultColWidthStr = attributes.getValue("defaultColWidth");
            if (defaultColWidthStr != null) {
                sheetStyle.setDefaultColumnWidth(Integer.valueOf(defaultColWidthStr));
            }
        }
        // "col" attr: min -> max 从1开始  width style
        else if ("col".equals(localName)) {
            int min = Integer.valueOf(attributes.getValue("min"));
            int max = Integer.valueOf(attributes.getValue("max"));
            for (int i = min - 1; i < max; i++) {
                String widthStr = attributes.getValue("width");
                if (widthStr != null && widthStr.length() > 0) {
                    sheetStyle.getColumnWidth().put(i, Float.valueOf(widthStr));
                }
                String styleStr = attributes.getValue("style");
                XSSFCellStyle colStyle = getCellStyle(styleStr);
                if (colStyle != null) {
                    sheetStyle.getColumnStyle().put(i, colStyle);
                }
            }
        }
        // "sheetData"
        else if ("sheetData".equals(localName)) {
            output.startSheetData(sheetStyle);
        }
        // "row" attr r s t ht
        else if ("row".equals(localName)) {
            String rowNumStr = attributes.getValue("r");
            if (rowNumStr != null) {
                rowNum = Integer.parseInt(rowNumStr) - 1;
            } else {
                rowNum = nextRowNum;
            }
            String styleStr = attributes.getValue("s");
            XSSFCellStyle rowStyle = getCellStyle(styleStr);
            String htStr = attributes.getValue("ht");
            // TODO 解析 t?
            output.startRow(rowNum, rowStyle, htStr == null ? null : Short.valueOf(htStr));
        }
        // c => cell
        else if ("c".equals(localName)) {
            // Set up defaults.
            this.nextDataType = xssfDataType.NUMBER;
            this.formatIndex = -1;
            this.formatString = null;
            cellRef = attributes.getValue("r");
            String cellType = attributes.getValue("t");
            String cellStyleStr = attributes.getValue("s");

            XSSFCellStyle style = null;
            if (stylesTable != null) {
                if (cellStyleStr != null) {
                    int styleIndex = Integer.parseInt(cellStyleStr);
                    style = stylesTable.getStyleAt(styleIndex);
                } else if (stylesTable.getNumCellStyles() > 0) {
                    style = stylesTable.getStyleAt(0);
                }
            }
            nextCellStyle = style;

            if ("b".equals(cellType))
                nextDataType = xssfDataType.BOOLEAN;
            else if ("e".equals(cellType))
                nextDataType = xssfDataType.ERROR;
            else if ("inlineStr".equals(cellType))
                nextDataType = xssfDataType.INLINE_STRING;
            else if ("s".equals(cellType))
                nextDataType = xssfDataType.SST_STRING;
            else if ("str".equals(cellType))
                nextDataType = xssfDataType.FORMULA;
            else {
                // Number, but almost certainly with a special style or format
                if (style != null) {
                    this.formatIndex = style.getDataFormat();
                    this.formatString = style.getDataFormatString();
                    if (this.formatString == null)
                        this.formatString = BuiltinFormats.getBuiltinFormat(this.formatIndex);
                }
            }
        }
        // text
        else if (isTextTag(localName)) {
            vIsOpen = true;
            // Clear contents cache
            value.setLength(0);
        } else if ("is".equals(localName)) {
            // Inline string outer tag
            isIsOpen = true;
        } else if ("f".equals(localName)) {
            // Clear contents cache
            formula.setLength(0);

            // Mark us as being a formula if not already
            if (nextDataType == xssfDataType.NUMBER) {
                nextDataType = xssfDataType.FORMULA;
            }

            // Decide where to get the formula string from
            String type = attributes.getValue("t");
            if (type != null && type.equals("shared")) {
                // Is it the one that defines the shared, or uses it?
                String ref = attributes.getValue("ref");
                String si = attributes.getValue("si");

                if (ref != null) {
                    // This one defines it
                    // TODO Save it somewhere
                    fIsOpen = true;
                } else {
                    // This one uses a shared formula
                    // TODO Retrieve the shared formula and tweak it to 
                    //  match the current cell
                    if (formulasNotResults) {
                        logger.log(POILogger.WARN, "shared formulas not yet supported!");
                    } /*else {
                   // It's a shared formula, so we can't get at the formula string yet
                   // However, they don't care about the formula string, so that's ok!
                }*/
                }
            } else {
                fIsOpen = true;
            }
        }
        // header footer
        else if ("oddHeader".equals(localName) || "evenHeader".equals(localName) ||
                "firstHeader".equals(localName) || "firstFooter".equals(localName) ||
                "oddFooter".equals(localName) || "evenFooter".equals(localName)) {
            hfIsOpen = true;
            // Clear contents cache
            headerFooter.setLength(0);
        }
        // "mergeCell"
        else if ("mergeCell".equals(localName) && attributes.getValue("ref") != null) {
            sheetStyle.getMergedRegions().add(CellRangeAddress.valueOf(attributes.getValue("ref")));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (uri != null && !uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        String thisStr = null;
        CellType cellType = null;

        // v => contents of a cell
        if (isTextTag(localName)) {
            vIsOpen = false;

            // Process the value contents as required, now we have it all
            switch (nextDataType) {
                case BOOLEAN:
                    char first = value.charAt(0);
                    thisStr = first == '0' ? "FALSE" : "TRUE";
                    cellType = CellType.BOOLEAN;
                    break;

                case ERROR:
                    thisStr = value.toString();
                    cellType = CellType.ERROR;
                    break;

                case FORMULA:
                    if (formulasNotResults) {
                        thisStr = formula.toString();
                    } else {
                        String fv = value.toString();

                        if (this.formatString != null) {
                            try {
                                // Try to use the value as a formattable number
                                double d = Double.parseDouble(fv);
                                thisStr = formatter.formatRawCellContents(d, this.formatIndex, this.formatString);
                            } catch (NumberFormatException e) {
                                // Formula is a String result not a Numeric one
                                thisStr = fv;
                            }
                        } else {
                            // No formatting applied, just do raw value in all cases
                            thisStr = fv;
                        }
                    }
                    cellType = CellType.FORMULA;
                    break;

                case INLINE_STRING:
                    // TODO: Can these ever have formatting on them?
                    XSSFRichTextString rtsi = new XSSFRichTextString(value.toString());
                    thisStr = rtsi.toString();
                    cellType = CellType.STRING;
                    break;

                case SST_STRING:
                    String sstIndex = value.toString();
                    try {
                        int idx = Integer.parseInt(sstIndex);
                        RichTextString rtss = sharedStringsTable.getItemAt(idx);
                        thisStr = rtss.toString();
                    } catch (NumberFormatException ex) {
                        logger.log(POILogger.ERROR, "Failed to parse SST index '" + sstIndex, ex);
                    }
                    cellType = CellType.STRING;
                    break;

                case NUMBER:
                    String n = value.toString();
                    if (this.formatString != null && n.length() > 0)
                        thisStr = formatter.formatRawCellContents(Double.parseDouble(n), this.formatIndex, this.formatString);
                    else
                        thisStr = n;
                    cellType = CellType.NUMERIC;
                    break;

                default:
                    thisStr = "(TODO: Unexpected type: " + nextDataType + ")";
                    break;
            }

            // Do we have a comment for this cell?
            checkForEmptyCellComments(EmptyCellCommentsCheckType.CELL);
            XSSFComment comment = comments != null ? comments.findCellComment(new CellAddress(cellRef)) : null;

            // Output
            output.cell(cellRef, thisStr, comment, nextCellStyle, cellType);
        } else if ("f".equals(localName)) {
            fIsOpen = false;
        } else if ("is".equals(localName)) {
            isIsOpen = false;
        } else if ("row".equals(localName)) {
            // Handle any "missing" cells which had comments attached
            checkForEmptyCellComments(EmptyCellCommentsCheckType.END_OF_ROW);

            // Finish up the row
            output.endRow(rowNum);

            // some sheets do not have rowNum set in the XML, Excel can read them so we should try to read them as well
            nextRowNum = rowNum + 1;
        } else if ("sheetData".equals(localName)) {
            // Handle any "missing" cells which had comments attached
            checkForEmptyCellComments(EmptyCellCommentsCheckType.END_OF_SHEET_DATA);

            output.endSheetData();
        } else if ("oddHeader".equals(localName) || "evenHeader".equals(localName) ||
                "firstHeader".equals(localName)) {
            hfIsOpen = false;
            output.headerFooter(headerFooter.toString(), true, localName);
        } else if ("oddFooter".equals(localName) || "evenFooter".equals(localName) ||
                "firstFooter".equals(localName)) {
            hfIsOpen = false;
            output.headerFooter(headerFooter.toString(), false, localName);
        } else if ("worksheet".equals(localName)) {
            output.endSheet(sheetStyle);
        }
    }

    /**
     * Captures characters only if a suitable element is open.
     * Originally was just "v"; extended for inlineStr also.
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (vIsOpen) {
            value.append(ch, start, length);
        }
        if (fIsOpen) {
            formula.append(ch, start, length);
        }
        if (hfIsOpen) {
            headerFooter.append(ch, start, length);
        }
    }

    /**
     * Do a check for, and output, comments in otherwise empty cells.
     */
    private void checkForEmptyCellComments(EmptyCellCommentsCheckType type) {
        if (commentCellRefs != null && !commentCellRefs.isEmpty()) {
            // If we've reached the end of the sheet data, output any
            //  comments we haven't yet already handled
            if (type == EmptyCellCommentsCheckType.END_OF_SHEET_DATA) {
                while (!commentCellRefs.isEmpty()) {
                    outputEmptyCellComment(commentCellRefs.remove());
                }
                return;
            }

            // At the end of a row, handle any comments for "missing" rows before us
            if (this.cellRef == null) {
                if (type == EmptyCellCommentsCheckType.END_OF_ROW) {
                    while (!commentCellRefs.isEmpty()) {
                        if (commentCellRefs.peek().getRow() == rowNum) {
                            outputEmptyCellComment(commentCellRefs.remove());
                        } else {
                            return;
                        }
                    }
                    return;
                } else {
                    throw new IllegalStateException("StyledCell ref should be null only if there are only empty cells in the row; rowNum: " + rowNum);
                }
            }

            CellAddress nextCommentCellRef;
            do {
                CellAddress cellRef = new CellAddress(this.cellRef);
                CellAddress peekCellRef = commentCellRefs.peek();
                if (type == EmptyCellCommentsCheckType.CELL && cellRef.equals(peekCellRef)) {
                    // remove the comment cell ref from the list if we're about to handle it alongside the cell content
                    commentCellRefs.remove();
                    return;
                } else {
                    // fill in any gaps if there are empty cells with comment mixed in with non-empty cells
                    int comparison = peekCellRef.compareTo(cellRef);
                    if (comparison > 0 && type == EmptyCellCommentsCheckType.END_OF_ROW && peekCellRef.getRow() <= rowNum) {
                        nextCommentCellRef = commentCellRefs.remove();
                        outputEmptyCellComment(nextCommentCellRef);
                    } else if (comparison < 0 && type == EmptyCellCommentsCheckType.CELL && peekCellRef.getRow() <= rowNum) {
                        nextCommentCellRef = commentCellRefs.remove();
                        outputEmptyCellComment(nextCommentCellRef);
                    } else {
                        nextCommentCellRef = null;
                    }
                }
            } while (nextCommentCellRef != null && !commentCellRefs.isEmpty());
        }
    }


    /**
     * Output an empty-cell comment.
     */
    private void outputEmptyCellComment(CellAddress cellRef) {
        XSSFComment comment = comments.findCellComment(cellRef);
        output.cell(cellRef.formatAsString(), null, comment, nextCellStyle, CellType.BLANK);
    }

    private enum EmptyCellCommentsCheckType {
        CELL,
        END_OF_ROW,
        END_OF_SHEET_DATA
    }

    /**
     * This interface allows to provide callbacks when reading
     * a sheet in streaming mode.
     * <p>
     * By implementing the methods, you can process arbitrarily
     * large files without exhausting main memory.
     */
    public interface SheetContentsHandler {

        default void startSheetData(SheetStyle sheetStyle) {
        }

        /**
         * A row with the (zero based) row number has started
         */
        void startRow(int rowNum, CellStyle rowStyle, Short height);

        /**
         * A row with the (zero based) row number has ended
         */
        void endRow(int rowNum);

        void cell(String cellReference, String formattedValue, XSSFComment comment, XSSFCellStyle cellStyle, CellType
                cellType);

        /**
         * A header or footer has been encountered
         */
        default void headerFooter(String text, boolean isHeader, String tagName) {
        }

        default void endSheetData() {
        }

        default void endSheet(SheetStyle sheetStyle) {
        }

    }

    private XSSFCellStyle getCellStyle(String cellStyleStr) {
        if (stylesTable != null) {
            if (cellStyleStr != null && cellStyleStr.length() > 0) {
                int styleIndex = Integer.parseInt(cellStyleStr);
                return stylesTable.getStyleAt(styleIndex);
            }
        }
        return null;
    }

    @Data
    @Accessors(chain = true)
    public static class StyledCell {
        // 0-based
        private int col;
        private String value;
        private CellStyle style;
        private CellType cellType;
    }

    @Data
    @Accessors(chain = true)
    public static class StyledRow {
        // 0-based
        private int rowNum;
        private Short height;
        private CellStyle style;
        private List<StyledCell> cells;
    }

    @Data
    @Accessors(chain = true)
    public static class SheetStyle {
        private Integer defaultColumnWidth;
        private Float defaultRowHeight;
        private Map<Integer, Float> columnWidth;
        private Map<Integer, CellStyle> columnStyle;
        // 该数据只当读取完成时有效，仅当新表中数据的行列号与原表相同时，才应该使用
        private List<CellRangeAddress> mergedRegions;
    }
}
