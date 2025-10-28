
- 简单的结构图 (TODO 补充完善 添加标签的属性 及相应的注释)
    ```xml
    <worksheet>
        <sheetPr />
        <dimension />
        <sheetViews>
            <sheetView>
                <selection />
            </sheetView>
        </sheetViews>
        <sheetFormatPr />
        <cols>
            <col />
        </cols>
        <sheetData>
            <row>
                <c>
                    <f></f>
                    <v></v>
                </c>
            </row>
        </sheetData>
        <mergeCells>
            <mergeCell />
        </mergeCells>
        <phoneticPr />
        <pageMargins />
        <pageSetup />
    </worksheet>
    ```
  
- 示例：
  ```xml
    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
      <!-- 工作表全局格式设置 -->
      <sheetFormatPr 
        defaultRowHeight="15" <!-- 默认行高15磅 -->
        defaultColWidth="8.43" <!-- 默认列宽8.43字符（Excel默认值） -->
        outlineLevelRow="0" 
        outlineLevelCol="0"
      />
    
      <!-- 列配置：第1-2列启用自定义宽度（覆盖defaultColWidth） -->
      <cols>
        <col min="1" max="1" width="15" customWidth="1"/>  <!-- 第1列宽15字符 -->
        <col min="2" max="2" width="10" customWidth="1"/>  <!-- 第2列宽10字符 -->
      </cols>
    
      <!-- 工作表数据 -->
      <sheetData>
        <row r="1">
          <c r="A1" t="s"><v>0</v></c> <!-- A1单元格（文本，关联sharedStrings.xml索引0） -->
          <c r="B1" t="s"><v>1</v></c> <!-- B1单元格（文本，关联索引1） -->
        </row>
        <row r="2">
          <c r="A2" t="s"><v>2</v></c> <!-- A2单元格（文本，索引2） -->
          <c r="B2" t="n"><v>25</v></c> <!-- B2单元格（数字25） -->
        </row>
      </sheetData>
    </worksheet>  

  ```

- [ppt讲解](http://download.microsoft.com/download/3/E/3/3E3435BD-AA68-4B32-B84D-B633F0D0F90D/SpreadsheetMLBasics.ppt)
- [What do excel xml cell attribute values mean?](https://stackoverflow.com/questions/18334314/what-do-excel-xml-cell-attribute-values-mean/18346273#18346273)