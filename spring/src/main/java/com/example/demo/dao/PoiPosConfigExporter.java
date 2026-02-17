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
import java.util.HashMap;
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

            int bCol = btnHm.require("ButtonColumnNumber");
            int bRow = btnHm.require("ButtonRowNumber");

            Map<String, PosConfig.Button> buttonsById = indexButtons(config);

            for (int r = btnHm.dataStartRow; r <= btn.getLastRowNum(); r++) {
                Row row = btn.getRow(r);
                if (row == null) continue;

                String buttonId = buttonRowId(r);
                PosConfig.Button button = buttonsById.get(buttonId);
                if (button == null) continue;

                setInt(row, bCol, button.getCol());
                setInt(row, bRow, button.getRow());
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    private static Map<String, PosConfig.Button> indexButtons(PosConfig config) {
        Map<String, PosConfig.Button> map = new HashMap<>();
        for (PosConfig.Page page : config.getPagesByPageNumber().values()) {
            for (PosConfig.Button button : page.getButtons()) {
                String id = button.getButtonId();
                if (id == null || id.isBlank()) {
                    throw new IllegalArgumentException("buttonId is missing");
                }
                map.put(id, button);
            }
        }
        return map;
    }

    private static void setInt(Row row, int colIndex, int value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value);
    }

    private static Sheet requireSheet(Workbook wb, String name) {
        Sheet s = wb.getSheet(name);
        if (s == null) throw new IllegalArgumentException("Sheet not found: " + name);
        return s;
    }

    private static String buttonRowId(int zeroBasedRowIndex) {
        return SHEET_BUTTON + "#R" + (zeroBasedRowIndex + 1);
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
}
