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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    @Test
    void export_updates_item_master_unit_price_from_button_edit() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        PosConfig.Button targetButton = findFirstButtonWithItemCode(config);
        String newUnitPrice = "9876";
        PosConfig updated = config.updateUnitPrice(
                findPageNumberOfButton(config, targetButton.getButtonId()),
                targetButton.getButtonId(),
                newUnitPrice
        );

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, updated);

        assertEquals(
                newUnitPrice,
                readItemMasterUnitPriceByItemCode(exported, targetButton.getItemCode()),
                "ItemMaster.UnitPrice was not updated"
        );
    }

    @Test
    void export_adds_category_row_to_preset_menu_master() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        String categoryName = "TEST_CATEGORY_ADD";
        PosConfig updated = config.addCategory(categoryName, 6, 4, 3);
        int addedPageNumber = updated.getCategories().stream()
                .mapToInt(PosConfig.Category::getPageNumber)
                .max()
                .orElseThrow();

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, updated);

        assertTrue(
                existsMenuRow(exported, addedPageNumber, 6, 4, categoryName, 3),
                "added category row was not written to PresetMenuMaster"
        );
    }

    @Test
    void export_deletes_category_row_from_preset_menu_master() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        PosConfig.Category targetCategory = config.getCategories().get(0);
        PosConfig updated = config.deleteCategory(targetCategory.getPageNumber());

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, updated);

        assertFalse(
                existsMenuRow(
                        exported,
                        targetCategory.getPageNumber(),
                        targetCategory.getCols(),
                        targetCategory.getRows(),
                        targetCategory.getName(),
                        targetCategory.getStyleKey()
                ),
                "deleted category row still exists in PresetMenuMaster"
        );
    }

    @Test
    void export_updates_item_category_master_display_level_from_handy_order() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);
        ItemCatalog handyCatalog = source.getHandyCatalog();

        ItemCatalog.Category targetCategory = findHandyCategoryWithAtLeastItems(handyCatalog, 3);
        List<ItemCatalog.Item> reorderedItems = new ArrayList<>(targetCategory.getItems());
        ItemCatalog.Item moved = reorderedItems.remove(0);
        reorderedItems.add(2, moved);

        List<ItemCatalog.Category> updatedCategories = new ArrayList<>(handyCatalog.getCategories());
        for (int i = 0; i < updatedCategories.size(); i++) {
            if (updatedCategories.get(i).getCode().equals(targetCategory.getCode())) {
                updatedCategories.set(
                        i,
                        new ItemCatalog.Category(
                                targetCategory.getCode(),
                                targetCategory.getDescription(),
                                reorderedItems
                        )
                );
                break;
            }
        }
        ItemCatalog updatedHandyCatalog = new ItemCatalog(updatedCategories);

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, config, updatedHandyCatalog);

        assertEquals(
                1,
                readItemCategoryDisplayLevel(exported, targetCategory.getCode(), reorderedItems.get(0).getItemCode())
        );
        assertEquals(
                2,
                readItemCategoryDisplayLevel(exported, targetCategory.getCode(), reorderedItems.get(1).getItemCode())
        );
        assertEquals(
                3,
                readItemCategoryDisplayLevel(exported, targetCategory.getCode(), reorderedItems.get(2).getItemCode())
        );
    }

    @Test
    void export_removes_item_category_master_row_when_handy_item_deleted() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);
        ItemCatalog handyCatalog = source.getHandyCatalog();

        HandyDeleteTarget target = findHandyDeleteTarget(handyCatalog);
        ItemCatalog.Category targetCategory = target.category();
        int beforeCount = countItemCategoryRows(originalBytes, targetCategory.getCode(), target.itemCode());

        List<ItemCatalog.Item> remainingItems = new ArrayList<>(targetCategory.getItems());
        remainingItems.remove(target.itemIndex());

        List<ItemCatalog.Category> updatedCategories = new ArrayList<>(handyCatalog.getCategories());
        for (int i = 0; i < updatedCategories.size(); i++) {
            if (updatedCategories.get(i).getCode().equals(targetCategory.getCode())) {
                updatedCategories.set(
                        i,
                        new ItemCatalog.Category(
                                targetCategory.getCode(),
                                targetCategory.getDescription(),
                                remainingItems
                        )
                );
                break;
            }
        }
        ItemCatalog updatedHandyCatalog = new ItemCatalog(updatedCategories);

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, config, updatedHandyCatalog);

        int afterCount = countItemCategoryRows(exported, targetCategory.getCode(), target.itemCode());
        assertEquals(beforeCount - 1, afterCount, "ItemCategoryMaster row count did not decrease");
    }

    @Test
    void export_adds_item_category_master_row_when_handy_item_added() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);
        ItemCatalog handyCatalog = source.getHandyCatalog();
        ItemCatalog itemCatalog = source.getItemCatalog();

        HandyAddTarget target = findHandyAddTarget(handyCatalog, itemCatalog);
        ItemCatalog.Category targetCategory = target.handyCategory();
        int beforeCount = countItemCategoryRows(originalBytes, targetCategory.getCode(), target.item().getItemCode());

        List<ItemCatalog.Item> items = new ArrayList<>(targetCategory.getItems());
        items.add(target.item());

        List<ItemCatalog.Category> updatedCategories = new ArrayList<>(handyCatalog.getCategories());
        for (int i = 0; i < updatedCategories.size(); i++) {
            if (updatedCategories.get(i).getCode().equals(targetCategory.getCode())) {
                updatedCategories.set(
                        i,
                        new ItemCatalog.Category(
                                targetCategory.getCode(),
                                targetCategory.getDescription(),
                                items
                        )
                );
                break;
            }
        }
        ItemCatalog updatedHandyCatalog = new ItemCatalog(updatedCategories);

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, config, updatedHandyCatalog);

        int afterCount = countItemCategoryRows(exported, targetCategory.getCode(), target.item().getItemCode());
        assertEquals(beforeCount + 1, afterCount, "ItemCategoryMaster row count did not increase");
        String afterName = readItemCategoryName(exported, targetCategory.getCode(), target.item().getItemCode());
        assertEquals(target.item().getItemName(), afterName, "ItemCategoryMaster name column was not updated");
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

    private static ItemCatalog.Category findHandyCategoryWithAtLeastItems(ItemCatalog catalog, int minItemCount) {
        for (ItemCatalog.Category category : catalog.getCategories()) {
            if (category.getItems().size() >= minItemCount) {
                return category;
            }
        }
        fail("no handy category has " + minItemCount + " or more items");
        return null;
    }

    private static HandyDeleteTarget findHandyDeleteTarget(ItemCatalog catalog) {
        for (ItemCatalog.Category category : catalog.getCategories()) {
            if (category.getItems().isEmpty()) continue;

            Map<String, Integer> itemCodeCounts = new HashMap<>();
            for (ItemCatalog.Item item : category.getItems()) {
                itemCodeCounts.merge(item.getItemCode(), 1, Integer::sum);
            }
            for (int i = 0; i < category.getItems().size(); i++) {
                String itemCode = category.getItems().get(i).getItemCode();
                if (itemCodeCounts.getOrDefault(itemCode, 0) == 1) {
                    return new HandyDeleteTarget(category, i, itemCode);
                }
            }
        }
        fail("no handy delete target found (unique itemCode required)");
        return null;
    }

    private static HandyAddTarget findHandyAddTarget(ItemCatalog handyCatalog, ItemCatalog itemCatalog) {
        for (ItemCatalog.Category handyCategory : handyCatalog.getCategories()) {
            Set<String> existingItemCodes = new HashSet<>();
            for (ItemCatalog.Item item : handyCategory.getItems()) {
                existingItemCodes.add(item.getItemCode());
            }

            for (ItemCatalog.Category sourceCategory : itemCatalog.getCategories()) {
                for (ItemCatalog.Item sourceItem : sourceCategory.getItems()) {
                    if (!existingItemCodes.contains(sourceItem.getItemCode())) {
                        return new HandyAddTarget(
                                handyCategory,
                                sourceItem
                        );
                    }
                }
            }
        }
        fail("no handy add target found");
        return null;
    }

    private static PosConfig.Button findFirstButtonWithItemCode(PosConfig config) {
        for (PosConfig.Page page : config.getPagesByPageNumber().values()) {
            for (PosConfig.Button button : page.getButtons()) {
                if (button.getItemCode() != null && !button.getItemCode().isBlank()) {
                    return button;
                }
            }
        }
        fail("no button with itemCode exists");
        return null;
    }

    private static int findPageNumberOfButton(PosConfig config, String buttonId) {
        for (Map.Entry<Integer, PosConfig.Page> entry : config.getPagesByPageNumber().entrySet()) {
            for (PosConfig.Button button : entry.getValue().getButtons()) {
                if (buttonId.equals(button.getButtonId())) {
                    return entry.getKey();
                }
            }
        }
        fail("buttonId not found: " + buttonId);
        return -1;
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

    private static String readItemMasterUnitPriceByItemCode(byte[] bytes, String itemCode) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("ItemMaster");
            if (sheet == null) fail("ItemMaster sheet not found");

            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, "ItemCode");
            if (headerRowIndex < 0) fail("ItemMaster header row not found");
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) fail("ItemMaster header row not found");

            Map<String, Integer> headerIndex = toHeaderIndexMap(headerRow, formatter);
            int hItemCode = requireColumn(headerIndex, "ItemCode");
            int hUnitPrice = requireColumn(headerIndex, "UnitPrice");

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String code = formatter.formatCellValue(row.getCell(hItemCode)).trim();
                if (!itemCode.equals(code)) continue;
                String price = formatter.formatCellValue(row.getCell(hUnitPrice)).trim();
                return normalizeNumericText(price);
            }
        }
        fail("itemCode not found in ItemMaster: " + itemCode);
        return "";
    }

    private static int readItemCategoryDisplayLevel(byte[] bytes, String categoryCode, String itemCode) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("ItemCategoryMaster");
            assertNotNull(sheet, "ItemCategoryMaster sheet not found");

            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, "CategoryCode");
            assertTrue(headerRowIndex >= 0, "ItemCategoryMaster header row not found");
            Row headerRow = sheet.getRow(headerRowIndex);
            assertNotNull(headerRow, "ItemCategoryMaster header row not found");

            Map<String, Integer> headerIndex = toHeaderIndexMap(headerRow, formatter);
            int hCategoryCode = requireColumn(headerIndex, "CategoryCode");
            int hItemCode = requireColumn(headerIndex, "ItemCode");
            int hDisplayLevel = requireColumn(headerIndex, "DisplayLevel");

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String rowCategoryCode = formatter.formatCellValue(row.getCell(hCategoryCode)).trim();
                String rowItemCode = formatter.formatCellValue(row.getCell(hItemCode)).trim();
                if (!categoryCode.equals(rowCategoryCode) || !itemCode.equals(rowItemCode)) continue;

                String displayLevel = formatter.formatCellValue(row.getCell(hDisplayLevel)).trim();
                assertFalse(displayLevel.isEmpty(), "DisplayLevel is empty");
                return toInt(displayLevel);
            }
        }

        fail("item row not found in ItemCategoryMaster: category=" + categoryCode + ", item=" + itemCode);
        return -1;
    }

    private static String readItemCategoryName(byte[] bytes, String categoryCode, String itemCode) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("ItemCategoryMaster");
            assertNotNull(sheet, "ItemCategoryMaster sheet not found");

            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, "CategoryCode");
            assertTrue(headerRowIndex >= 0, "ItemCategoryMaster header row not found");
            Row headerRow = sheet.getRow(headerRowIndex);
            assertNotNull(headerRow, "ItemCategoryMaster header row not found");

            Map<String, Integer> headerIndex = toHeaderIndexMap(headerRow, formatter);
            int hCategoryCode = requireColumn(headerIndex, "CategoryCode");
            int hItemCode = requireColumn(headerIndex, "ItemCode");
            int hDisplayLevel = requireColumn(headerIndex, "DisplayLevel");
            int hName = headerIndex.getOrDefault("Description", hDisplayLevel + 1);

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String rowCategoryCode = formatter.formatCellValue(row.getCell(hCategoryCode)).trim();
                String rowItemCode = formatter.formatCellValue(row.getCell(hItemCode)).trim();
                if (!categoryCode.equals(rowCategoryCode) || !itemCode.equals(rowItemCode)) continue;
                return formatter.formatCellValue(row.getCell(hName)).trim();
            }
        }

        fail("item row not found in ItemCategoryMaster: category=" + categoryCode + ", item=" + itemCode);
        return "";
    }

    private static int countItemCategoryRows(byte[] bytes, String categoryCode, String itemCode) throws Exception {
        int count = 0;
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("ItemCategoryMaster");
            assertNotNull(sheet, "ItemCategoryMaster sheet not found");

            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, "CategoryCode");
            assertTrue(headerRowIndex >= 0, "ItemCategoryMaster header row not found");
            Row headerRow = sheet.getRow(headerRowIndex);
            assertNotNull(headerRow, "ItemCategoryMaster header row not found");

            Map<String, Integer> headerIndex = toHeaderIndexMap(headerRow, formatter);
            int hCategoryCode = requireColumn(headerIndex, "CategoryCode");
            int hItemCode = requireColumn(headerIndex, "ItemCode");

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String rowCategoryCode = formatter.formatCellValue(row.getCell(hCategoryCode)).trim();
                String rowItemCode = formatter.formatCellValue(row.getCell(hItemCode)).trim();
                if (categoryCode.equals(rowCategoryCode) && itemCode.equals(rowItemCode)) {
                    count += 1;
                }
            }
        }
        return count;
    }

    private static String normalizeNumericText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.endsWith(".0")) {
            return trimmed.substring(0, trimmed.length() - 2);
        }
        return trimmed;
    }

    private static boolean existsMenuRow(
            byte[] bytes,
            int pageNumber,
            int cols,
            int rows,
            String description,
            int styleKey
    ) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("PresetMenuMaster");
            if (sheet == null) return false;

            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, "PageNumber");
            if (headerRowIndex < 0) return false;
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) return false;

            Map<String, Integer> headerIndex = toHeaderIndexMap(headerRow, formatter);
            int hPage = requireColumn(headerIndex, "PageNumber");
            int hCols = requireColumn(headerIndex, "ButtonColumnCount");
            int hRows = requireColumn(headerIndex, "ButtonRowCount");
            int hDesc = requireColumn(headerIndex, "Description");
            int hStyle = requireColumn(headerIndex, "StyleKey");

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String p = formatter.formatCellValue(row.getCell(hPage)).trim();
                if (p.isEmpty()) continue;
                int page = toInt(p);
                int c = toInt(formatter.formatCellValue(row.getCell(hCols)).trim());
                int rr = toInt(formatter.formatCellValue(row.getCell(hRows)).trim());
                String d = formatter.formatCellValue(row.getCell(hDesc)).trim();
                int s = toInt(formatter.formatCellValue(row.getCell(hStyle)).trim());
                if (page == pageNumber && c == cols && rr == rows && d.equals(description) && s == styleKey) {
                    return true;
                }
            }
            return false;
        }
    }

    private record TargetCell(int pageNumber, int col, int row) {
    }

    private record MoveTarget(int pageNumber, PosConfig.Button sourceButton, int emptyCol, int emptyRow) {
    }

    private record HandyDeleteTarget(ItemCatalog.Category category, int itemIndex, String itemCode) {
    }

    private record HandyAddTarget(
            ItemCatalog.Category handyCategory,
            ItemCatalog.Item item
    ) {
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
