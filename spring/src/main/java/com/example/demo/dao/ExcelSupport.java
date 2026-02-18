package com.example.demo.dao;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class ExcelSupport {
    private ExcelSupport() {
    }

    static Integer firstExisting(ExcelSupport.HeaderMap headerMap, String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            Integer found = headerMap.get(candidate);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static final class ExcelUtil {
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

    static final class HeaderMap {
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

        Integer get(String name) {
            return col.get(name);
        }

        static HeaderMap from(Sheet sheet, ExcelUtil u, String... headerCandidates) {
            int headerRow = findHeaderRow(sheet, u, headerCandidates);
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

        static int findHeaderRow(Sheet sheet, ExcelUtil u, String... headerCandidates) {
            int max = Math.min(sheet.getLastRowNum(), 20);
            for (int r = 0; r <= max; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String v = u.str(row.getCell(c));
                    if (v == null) continue;
                    String t = v.trim();
                    if (headerCandidates != null) {
                        for (String candidate : headerCandidates) {
                            if (candidate != null && !candidate.isBlank() && t.equals(candidate)) {
                                return r;
                            }
                        }
                    }
                }
            }
            return 0;
        }
    }
}
