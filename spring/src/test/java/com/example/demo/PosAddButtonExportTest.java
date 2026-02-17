package com.example.demo;

import com.example.demo.dao.PoiPosConfigExporter;
import com.example.demo.dao.PoiPosConfigReader;
import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PosAddButtonExportTest {

    private static final Path ORIGINAL = Path.of("..", "2026ウインターフェア.xlsx");

    @Test
    void export_writes_new_button_row_for_added_cell() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        TargetCell targetCell = findFirstEmptyCell(config);
        ItemCatalog.Item item = firstCatalogItem(source.getItemCatalog());

        String newLabel = "TEST_ADD_BUTTON";
        String newButtonId = "PresetMenuButtonMaster#NEW-TEST";
        PosConfig updated = config.addButton(
                targetCell.pageNumber(),
                targetCell.col(),
                targetCell.row(),
                newLabel,
                1,
                item.getItemCode(),
                item.getUnitPrice(),
                newButtonId
        );

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, updated);

        assertTrue(
                existsButtonRow(
                        exported,
                        targetCell.pageNumber(),
                        targetCell.col(),
                        targetCell.row(),
                        newLabel,
                        item.getItemCode()
                ),
                "newly added button row was not written to PresetMenuButtonMaster"
        );

        assertDataRowsAreSortedAndContiguous(exported);
    }

    @Test
    void export_removes_deleted_button_row() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        PosConfig.Category firstCategory = config.getCategories().get(0);
        PosConfig.Page firstPage = config.getPage(firstCategory.getPageNumber());
        assertNotNull(firstPage, "first page not found");
        assertFalse(firstPage.getButtons().isEmpty(), "first page has no buttons");

        PosConfig.Button target = firstPage.getButtons().get(0);
        PosConfig updated = config.deleteButton(firstPage.getPageNumber(), target.getButtonId());

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, updated);

        assertFalse(
                existsButtonRow(
                        exported,
                        firstPage.getPageNumber(),
                        target.getCol(),
                        target.getRow(),
                        target.getLabel(),
                        target.getItemCode()
                ),
                "deleted button row still exists in PresetMenuButtonMaster"
        );

        assertDataRowsAreSortedAndContiguous(exported);
    }

    @Test
    void export_moves_button_when_target_cell_is_empty() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        MoveTarget target = findMoveTarget(config);
        PosConfig.Button sourceButton = target.sourceButton();

        PosConfig updated = config.swapButtons(
                target.pageNumber(),
                sourceButton.getCol(),
                sourceButton.getRow(),
                target.emptyCol(),
                target.emptyRow()
        );

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, updated);

        assertTrue(
                existsButtonRow(
                        exported,
                        target.pageNumber(),
                        target.emptyCol(),
                        target.emptyRow(),
                        sourceButton.getLabel(),
                        sourceButton.getItemCode()
                ),
                "moved button row was not found at target empty cell"
        );

        assertFalse(
                existsButtonRow(
                        exported,
                        target.pageNumber(),
                        sourceButton.getCol(),
                        sourceButton.getRow(),
                        sourceButton.getLabel(),
                        sourceButton.getItemCode()
                ),
                "moved button row still remains at source cell"
        );

        assertDataRowsAreSortedAndContiguous(exported);
    }

    private static TargetCell findFirstEmptyCell(PosConfig config) {
        for (PosConfig.Category category : config.getCategories()) {
            PosConfig.Page page = config.getPage(category.getPageNumber());
            if (page == null) continue;

            Set<String> occupied = new HashSet<>();
            for (PosConfig.Button button : page.getButtons()) {
                occupied.add(button.getCol() + "-" + button.getRow());
            }

            for (int row = 1; row <= page.getRows(); row++) {
                for (int col = 1; col <= page.getCols(); col++) {
                    String key = col + "-" + row;
                    if (!occupied.contains(key)) {
                        return new TargetCell(page.getPageNumber(), col, row);
                    }
                }
            }
        }
        fail("no empty cell exists");
        return null;
    }

    private static MoveTarget findMoveTarget(PosConfig config) {
        for (PosConfig.Category category : config.getCategories()) {
            PosConfig.Page page = config.getPage(category.getPageNumber());
            if (page == null || page.getButtons().isEmpty()) continue;

            Set<String> occupied = new HashSet<>();
            for (PosConfig.Button button : page.getButtons()) {
                occupied.add(button.getCol() + "-" + button.getRow());
            }

            for (int row = 1; row <= page.getRows(); row++) {
                for (int col = 1; col <= page.getCols(); col++) {
                    String key = col + "-" + row;
                    if (!occupied.contains(key)) {
                        return new MoveTarget(page.getPageNumber(), page.getButtons().get(0), col, row);
                    }
                }
            }
        }
        fail("no page has both button and empty cell");
        return null;
    }

    private static ItemCatalog.Item firstCatalogItem(ItemCatalog catalog) {
        for (ItemCatalog.Category category : catalog.getCategories()) {
            if (!category.getItems().isEmpty()) {
                return category.getItems().get(0);
            }
        }
        fail("catalog has no items");
        return null;
    }

    private static boolean existsButtonRow(
            byte[] bytes,
            int page,
            int col,
            int rowNo,
            String description,
            String itemCode
    ) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("PresetMenuButtonMaster");
            if (sheet == null) return false;

            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, "PageNumber");
            if (headerRowIndex < 0) return false;
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) return false;

            Map<String, Integer> headerIndex = toHeaderIndexMap(headerRow, formatter);
            int hPage = requireColumn(headerIndex, "PageNumber");
            int hCol = requireColumn(headerIndex, "ButtonColumnNumber");
            int hRow = requireColumn(headerIndex, "ButtonRowNumber");
            int hDesc = requireColumn(headerIndex, "Description");
            int hSet = requireColumn(headerIndex, "SettingData");

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null) continue;
                String p = formatter.formatCellValue(dataRow.getCell(hPage)).trim();
                if (p.isEmpty()) continue;
                String c = formatter.formatCellValue(dataRow.getCell(hCol)).trim();
                String rr = formatter.formatCellValue(dataRow.getCell(hRow)).trim();
                String d = formatter.formatCellValue(dataRow.getCell(hDesc)).trim();
                String s = formatter.formatCellValue(dataRow.getCell(hSet)).trim();

                if (toInt(p) == page && toInt(c) == col && toInt(rr) == rowNo && d.equals(description) && s.equals(itemCode)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static int findHeaderRowIndex(Sheet sheet, DataFormatter formatter, String headerName) {
        int max = Math.min(sheet.getLastRowNum(), 20);
        for (int r = 0; r <= max; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String text = formatter.formatCellValue(row.getCell(c)).trim();
                if (headerName.equals(text)) {
                    return r;
                }
            }
        }
        return -1;
    }

    private static Map<String, Integer> toHeaderIndexMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String text = formatter.formatCellValue(headerRow.getCell(c)).trim();
            if (!text.isEmpty()) {
                map.put(text, c);
            }
        }
        return map;
    }

    private static int requireColumn(Map<String, Integer> map, String name) {
        Integer idx = map.get(name);
        if (idx == null) throw new IllegalArgumentException("header not found: " + name);
        return idx;
    }

    private static int toInt(String text) {
        String normalized = text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        return Integer.parseInt(normalized);
    }

    private static void assertDataRowsAreSortedAndContiguous(byte[] bytes) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("PresetMenuButtonMaster");
            assertNotNull(sheet, "PresetMenuButtonMaster sheet not found");

            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, "PageNumber");
            assertTrue(headerRowIndex >= 0, "header row not found");
            Row headerRow = sheet.getRow(headerRowIndex);
            assertNotNull(headerRow, "header row not found");

            Map<String, Integer> headerIndex = toHeaderIndexMap(headerRow, formatter);
            int hPage = requireColumn(headerIndex, "PageNumber");
            int hCol = requireColumn(headerIndex, "ButtonColumnNumber");
            int hRow = requireColumn(headerIndex, "ButtonRowNumber");

            boolean seenData = false;
            boolean seenBlankAfterData = false;
            ButtonKey prev = null;

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null) continue;
                String p = formatter.formatCellValue(dataRow.getCell(hPage)).trim();
                if (p.isEmpty()) {
                    if (seenData) {
                        seenBlankAfterData = true;
                    }
                    continue;
                }

                if (seenBlankAfterData) {
                    fail("found non-empty row after blank rows at physical row " + (r + 1));
                }

                int page = toInt(p);
                int col = toInt(formatter.formatCellValue(dataRow.getCell(hCol)).trim());
                int rowNo = toInt(formatter.formatCellValue(dataRow.getCell(hRow)).trim());
                ButtonKey current = new ButtonKey(page, col, rowNo);

                if (prev != null) {
                    assertTrue(
                            prev.compareTo(current) <= 0,
                            "rows are not sorted by (PageNumber,ButtonColumnNumber,ButtonRowNumber) at physical row " + (r + 1)
                    );
                }
                prev = current;
                seenData = true;
            }
        }
    }

    private record TargetCell(int pageNumber, int col, int row) {
    }

    private record MoveTarget(int pageNumber, PosConfig.Button sourceButton, int emptyCol, int emptyRow) {
    }

    private record ButtonKey(int page, int col, int row) implements Comparable<ButtonKey> {
        @Override
        public int compareTo(ButtonKey other) {
            int pageCompare = Integer.compare(this.page, other.page);
            if (pageCompare != 0) return pageCompare;
            int colCompare = Integer.compare(this.col, other.col);
            if (colCompare != 0) return colCompare;
            return Integer.compare(this.row, other.row);
        }
    }
}
