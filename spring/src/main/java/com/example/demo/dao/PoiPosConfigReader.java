package com.example.demo.dao;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import com.example.demo.service.port.PosConfigReader;

import java.io.InputStream;
import java.util.*;

@Component
public class PoiPosConfigReader implements PosConfigReader {

    private static final String SHEET_MENU = "PresetMenuMaster";
    private static final String SHEET_BUTTON = "PresetMenuButtonMaster";

    @Override
    public PosConfigSource read(InputStream in) throws Exception {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet menu = requireSheet(wb, SHEET_MENU);
            Sheet btn  = requireSheet(wb, SHEET_BUTTON);

            ExcelUtil u = new ExcelUtil(wb);

            HeaderMap menuHm = HeaderMap.from(menu, u);
            HeaderMap btnHm  = HeaderMap.from(btn, u);

            // PresetMenuMaster: PageNumber, ButtonColumnCount, ButtonRowCount, Description, StyleKey
            int mPage = menuHm.require("PageNumber");
            int mCols = menuHm.require("ButtonColumnCount");
            int mRows = menuHm.require("ButtonRowCount");
            int mDesc = menuHm.require("Description");
            int mStyle= menuHm.require("StyleKey");

            List<PosConfig.Category> categories = new ArrayList<>();
            for (int r = menuHm.dataStartRow; r <= menu.getLastRowNum(); r++) {
                Row row = menu.getRow(r);
                if (row == null) continue;

                String pageS = u.str(row.getCell(mPage));
                if (isBlank(pageS)) continue;

                int page = parseIntStrict(pageS, "PresetMenuMaster.PageNumber");
                int cols = parseIntStrict(u.str(row.getCell(mCols)), "PresetMenuMaster.ButtonColumnCount");
                int rows = parseIntStrict(u.str(row.getCell(mRows)), "PresetMenuMaster.ButtonRowCount");
                String desc = nonNull(u.str(row.getCell(mDesc)));
                int style = parseIntStrict(u.str(row.getCell(mStyle)), "PresetMenuMaster.StyleKey");

                categories.add(new PosConfig.Category(page, cols, rows, desc, style));
            }

            // PresetMenuButtonMaster: PageNumber, ButtonColumnNumber, ButtonRowNumber, Description, StyleKey, SettingData
            int bPage = btnHm.require("PageNumber");
            int bCol  = btnHm.require("ButtonColumnNumber");
            int bRow  = btnHm.require("ButtonRowNumber");
            int bDesc = btnHm.require("Description");
            int bStyle= btnHm.require("StyleKey");
            int bSet  = btnHm.require("SettingData");

            List<PosConfigSource.PageButton> pageButtons = new ArrayList<>();
            for (int r = btnHm.dataStartRow; r <= btn.getLastRowNum(); r++) {
                Row row = btn.getRow(r);
                if (row == null) continue;

                String pageS = u.str(row.getCell(bPage));
                if (isBlank(pageS)) continue;

                int page = parseIntStrict(pageS, "PresetMenuButtonMaster.PageNumber");
                int col  = parseIntStrict(u.str(row.getCell(bCol)), "PresetMenuButtonMaster.ButtonColumnNumber");
                int rowNo= parseIntStrict(u.str(row.getCell(bRow)), "PresetMenuButtonMaster.ButtonRowNumber");
                String desc = nonNull(u.str(row.getCell(bDesc)));
                int style = parseIntStrict(u.str(row.getCell(bStyle)), "PresetMenuButtonMaster.StyleKey");
                String itemCode = nonNull(u.str(row.getCell(bSet))); // SettingData

                pageButtons.add(
                        new PosConfigSource.PageButton(
                                page,
                                new PosConfig.Button(col, rowNo, desc, style, itemCode)
                        )
                );
            }

            return new PosConfigSource(categories, pageButtons);
        }
    }

    private static Sheet requireSheet(Workbook wb, String name) {
        Sheet s = wb.getSheet(name);
        if (s == null) throw new IllegalArgumentException("Sheet not found: " + name);
        return s;
    }

    // ---- helpers ----
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String nonNull(String s) { return s == null ? "" : s; }

    private static int parseIntStrict(String s, String field) {
        if (s == null) throw new IllegalArgumentException("Missing value: " + field);
        String t = s.trim();
        if (t.matches("^-?\\d+\\.0$")) t = t.substring(0, t.length() - 2);
        if (!t.matches("^-?\\d+$")) throw new IllegalArgumentException("Not an int (" + field + "): " + s);
        return Integer.parseInt(t);
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
