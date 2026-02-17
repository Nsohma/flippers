package com.example.demo.dao;

import com.example.demo.model.PosConfig;
import com.example.demo.service.port.PosConfigExporter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PoiPosConfigExporter implements PosConfigExporter {
    private static final String SHEET_BUTTON = "PresetMenuButtonMaster";

    @Override
    public byte[] export(byte[] originalExcelBytes, PosConfig config) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(originalExcelBytes))) {
            Sheet btn = requireSheet(wb, SHEET_BUTTON);
            ExcelUtil u = new ExcelUtil(wb);
            HeaderMap btnHm = HeaderMap.from(btn, u);

            int bPage = btnHm.require("PageNumber");
            int bCol = btnHm.require("ButtonColumnNumber");
            int bRow = btnHm.require("ButtonRowNumber");
            int bDesc = btnHm.require("Description");
            int bStyle = btnHm.require("StyleKey");
            int bSet = btnHm.require("SettingData");

            List<IndexedButton> sortedButtons = collectAndSortButtons(config);

            for (int r = btnHm.dataStartRow; r <= btn.getLastRowNum(); r++) {
                Row row = btn.getRow(r);
                if (row == null) continue;
                clearButtonRow(row, bPage, bCol, bRow, bDesc, bStyle, bSet);
            }

            int writeRowIndex = btnHm.dataStartRow;
            for (IndexedButton indexedButton : sortedButtons) {
                Row row = btn.getRow(writeRowIndex);
                if (row == null) {
                    row = btn.createRow(writeRowIndex);
                }
                writeButtonRow(row, bPage, bCol, bRow, bDesc, bStyle, bSet, indexedButton);
                writeRowIndex++;
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    private static List<IndexedButton> collectAndSortButtons(PosConfig config) {
        List<IndexedButton> list = new ArrayList<>();
        for (Map.Entry<Integer, PosConfig.Page> pageEntry : config.getPagesByPageNumber().entrySet()) {
            int pageNumber = pageEntry.getKey();
            PosConfig.Page page = pageEntry.getValue();
            for (PosConfig.Button button : page.getButtons()) {
                list.add(new IndexedButton(pageNumber, button));
            }
        }
        list.sort(
                Comparator
                        .comparingInt(IndexedButton::pageNumber)
                        .thenComparingInt(v -> v.button().getCol())
                        .thenComparingInt(v -> v.button().getRow())
                        .thenComparing(v -> safe(v.button().getItemCode()))
                        .thenComparing(v -> safe(v.button().getButtonId()))
        );
        return list;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void clearButtonRow(
            Row row,
            int bPage,
            int bCol,
            int bRow,
            int bDesc,
            int bStyle,
            int bSet
    ) {
        clearCell(row, bPage);
        clearCell(row, bCol);
        clearCell(row, bRow);
        clearCell(row, bDesc);
        clearCell(row, bStyle);
        clearCell(row, bSet);
    }

    private static void writeButtonRow(
            Row row,
            int bPage,
            int bCol,
            int bRow,
            int bDesc,
            int bStyle,
            int bSet,
            IndexedButton indexedButton
    ) {
        PosConfig.Button button = indexedButton.button();
        setInt(row, bPage, indexedButton.pageNumber());
        setInt(row, bCol, button.getCol());
        setInt(row, bRow, button.getRow());
        setString(row, bDesc, button.getLabel());
        setInt(row, bStyle, button.getStyleKey());
        setString(row, bSet, button.getItemCode());
    }

    private static void setInt(Row row, int colIndex, int value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value);
    }

    private static void setString(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value == null ? "" : value);
    }

    private static void clearCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return;
        cell.setBlank();
    }

    private static Sheet requireSheet(Workbook wb, String name) {
        Sheet s = wb.getSheet(name);
        if (s == null) throw new IllegalArgumentException("Sheet not found: " + name);
        return s;
    }

    static class ExcelUtil {
        private final DataFormatter fmt = new DataFormatter(Locale.ROOT);
        private final FormulaEvaluator eval;

        ExcelUtil(Workbook wb) {
            this.eval = wb.getCreationHelper().createFormulaEvaluator();
        }

        String str(Cell cell) {
            if (cell == null) return null;
            return fmt.formatCellValue(cell, eval).trim();
        }
    }

    static class HeaderMap {
        final Map<String, Integer> col;
        final int dataStartRow;

        HeaderMap(Map<String, Integer> col, int dataStartRow) {
            this.col = col;
            this.dataStartRow = dataStartRow;
        }

        int require(String name) {
            Integer idx = col.get(name);
            if (idx == null) throw new IllegalArgumentException("Missing column '" + name + "'. headers=" + col.keySet());
            return idx;
        }

        static HeaderMap from(Sheet sheet, ExcelUtil u) {
            int headerRow = findHeaderRow(sheet, u);
            Row hr = sheet.getRow(headerRow);
            if (hr == null) throw new IllegalArgumentException("Header row not found: " + sheet.getSheetName());

            Map<String, Integer> map = new HashMap<>();
            for (int c = 0; c < hr.getLastCellNum(); c++) {
                String h = u.str(hr.getCell(c));
                if (h == null) continue;
                String key = h.trim();
                if (!key.isEmpty()) map.put(key, c);
            }
            return new HeaderMap(map, headerRow + 1);
        }

        static int findHeaderRow(Sheet sheet, ExcelUtil u) {
            int max = Math.min(sheet.getLastRowNum(), 20);
            for (int r = 0; r <= max; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String v = u.str(row.getCell(c));
                    if (v == null) continue;
                    String t = v.trim();
                    if (t.equals("PageNumber")) return r;
                }
            }
            return 0;
        }
    }

    private record IndexedButton(int pageNumber, PosConfig.Button button) {
    }
}
