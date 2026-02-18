package com.example.demo.dao;

import com.example.demo.dao.ExcelSupport.ExcelUtil;
import com.example.demo.dao.ExcelSupport.HeaderMap;
import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.service.port.PosConfigExporter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PoiPosConfigExporter implements PosConfigExporter {
    private static final String SHEET_BUTTON = "PresetMenuButtonMaster";
    private static final String SHEET_MENU = "PresetMenuMaster";
    private static final String SHEET_ITEM = "ItemMaster";
    private static final String SHEET_CATEGORY = "CategoryMaster";
    private static final String SHEET_ITEM_CATEGORY = "ItemCategoryMaster";

    @Override
    public byte[] export(byte[] originalExcelBytes, PosConfig config) throws Exception {
        return export(originalExcelBytes, config, null);
    }

    @Override
    public byte[] export(byte[] originalExcelBytes, PosConfig config, ItemCatalog handyCatalog) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(originalExcelBytes))) {
            Sheet btn = requireSheet(wb, SHEET_BUTTON);
            Sheet menu = requireSheet(wb, SHEET_MENU);
            Sheet item = requireSheet(wb, SHEET_ITEM);
            ExcelUtil u = new ExcelUtil(wb);
            HeaderMap btnHm = HeaderMap.from(btn, u, "PageNumber");
            HeaderMap menuHm = HeaderMap.from(menu, u, "PageNumber");
            HeaderMap itemHm = HeaderMap.from(item, u, "ItemCode", "UnitPrice");

            int bPage = btnHm.require("PageNumber");
            int bCol = btnHm.require("ButtonColumnNumber");
            int bRow = btnHm.require("ButtonRowNumber");
            int bDesc = btnHm.require("Description");
            int bStyle = btnHm.require("StyleKey");
            int bSet = btnHm.require("SettingData");
            int mPage = menuHm.require("PageNumber");
            int mCols = menuHm.require("ButtonColumnCount");
            int mRows = menuHm.require("ButtonRowCount");
            int mDesc = menuHm.require("Description");
            int mStyle = menuHm.require("StyleKey");
            int iCode = itemHm.require("ItemCode");
            int iUnitPrice = itemHm.require("UnitPrice");

            List<IndexedButton> sortedButtons = collectAndSortButtons(config);
            List<PosConfig.Category> sortedCategories = collectAndSortCategories(config);

            for (int r = menuHm.dataStartRow; r <= menu.getLastRowNum(); r++) {
                Row row = menu.getRow(r);
                if (row == null) continue;
                clearMenuRow(row, mPage, mCols, mRows, mDesc, mStyle);
            }
            int writeMenuRowIndex = menuHm.dataStartRow;
            for (PosConfig.Category category : sortedCategories) {
                Row row = menu.getRow(writeMenuRowIndex);
                if (row == null) {
                    row = menu.createRow(writeMenuRowIndex);
                }
                writeMenuRow(row, mPage, mCols, mRows, mDesc, mStyle, category);
                writeMenuRowIndex++;
            }

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

            Map<String, String> unitPriceByItemCode = collectUnitPriceByItemCode(config);
            for (int r = itemHm.dataStartRow; r <= item.getLastRowNum(); r++) {
                Row row = item.getRow(r);
                if (row == null) continue;

                String itemCode = u.str(row.getCell(iCode));
                if (itemCode == null || itemCode.isBlank()) continue;
                String unitPrice = unitPriceByItemCode.get(itemCode);
                if (unitPrice == null || unitPrice.isBlank()) continue;
                setNumericOrString(row, iUnitPrice, unitPrice);
            }

            applyHandyCategoryDisplayLevels(wb, u, handyCatalog);
            applyHandyDisplayLevels(wb, u, handyCatalog);

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

    private static Map<String, String> collectUnitPriceByItemCode(PosConfig config) {
        Map<String, String> map = new HashMap<>();
        for (PosConfig.Page page : config.getPagesByPageNumber().values()) {
            for (PosConfig.Button button : page.getButtons()) {
                String itemCode = button.getItemCode();
                String unitPrice = button.getUnitPrice();
                if (itemCode == null || itemCode.isBlank()) continue;
                if (unitPrice == null || unitPrice.isBlank()) continue;
                map.put(itemCode, unitPrice);
            }
        }
        return map;
    }

    private static List<PosConfig.Category> collectAndSortCategories(PosConfig config) {
        List<PosConfig.Category> list = new ArrayList<>(config.getCategories());
        list.sort(Comparator.comparingInt(PosConfig.Category::getPageNumber));
        return list;
    }

    private static void applyHandyCategoryDisplayLevels(Workbook wb, ExcelUtil u, ItemCatalog handyCatalog) {
        if (handyCatalog == null) {
            return;
        }

        Sheet categoryMaster = wb.getSheet(SHEET_CATEGORY);
        if (categoryMaster == null) {
            return;
        }

        HeaderMap categoryHm = HeaderMap.from(categoryMaster, u, "CategoryCode", "DisplayLevel");
        int hCategoryCode = categoryHm.require("CategoryCode");
        int hDisplayLevel = categoryHm.require("DisplayLevel");
        Integer hDescription = ExcelSupport.firstExisting(
                categoryHm,
                "Description",
                "CategoryName",
                "CategoryDescription"
        );
        if (hDescription == null) {
            hDescription = hDisplayLevel + 1;
        }

        Map<String, DesiredHandyCategory> desiredByCategoryCode = new LinkedHashMap<>();
        int displayLevel = 1;
        for (ItemCatalog.Category category : handyCatalog.getCategories()) {
            String categoryCode = safe(category.getCode());
            if (categoryCode.isBlank()) {
                continue;
            }
            desiredByCategoryCode.putIfAbsent(
                    categoryCode,
                    new DesiredHandyCategory(displayLevel, resolveHandyCategoryName(category))
            );
            displayLevel += 1;
        }

        Map<String, Boolean> writtenCategories = new HashMap<>();
        List<Integer> rowsToDelete = new ArrayList<>();
        for (int r = categoryHm.dataStartRow; r <= categoryMaster.getLastRowNum(); r++) {
            Row row = categoryMaster.getRow(r);
            if (row == null) continue;

            String categoryCode = safe(u.str(row.getCell(hCategoryCode)));
            if (categoryCode.isBlank()) {
                continue;
            }

            DesiredHandyCategory desired = desiredByCategoryCode.get(categoryCode);
            if (desired == null || writtenCategories.containsKey(categoryCode)) {
                rowsToDelete.add(r);
                continue;
            }

            setInt(row, hDisplayLevel, desired.displayLevel());
            setString(row, hDescription, desired.description());
            writtenCategories.put(categoryCode, true);
        }

        for (int i = rowsToDelete.size() - 1; i >= 0; i--) {
            deleteRow(categoryMaster, rowsToDelete.get(i));
        }

        for (Map.Entry<String, DesiredHandyCategory> entry : desiredByCategoryCode.entrySet()) {
            String categoryCode = entry.getKey();
            if (writtenCategories.containsKey(categoryCode)) {
                continue;
            }

            int rowIndex = Math.max(categoryHm.dataStartRow, categoryMaster.getLastRowNum() + 1);
            Row row = categoryMaster.getRow(rowIndex);
            if (row == null) {
                row = categoryMaster.createRow(rowIndex);
            }
            setString(row, hCategoryCode, categoryCode);
            setInt(row, hDisplayLevel, entry.getValue().displayLevel());
            setString(row, hDescription, entry.getValue().description());
        }
    }

    private static void applyHandyDisplayLevels(Workbook wb, ExcelUtil u, ItemCatalog handyCatalog) {
        if (handyCatalog == null) {
            return;
        }

        Sheet itemCategory = wb.getSheet(SHEET_ITEM_CATEGORY);
        if (itemCategory == null) {
            return;
        }

        HeaderMap itemCategoryHm = HeaderMap.from(itemCategory, u, "CategoryCode", "ItemCode", "DisplayLevel");
        int hCategoryCode = itemCategoryHm.require("CategoryCode");
        int hItemCode = itemCategoryHm.require("ItemCode");
        int hDisplayLevel = itemCategoryHm.require("DisplayLevel");
        Integer hItemName = ExcelSupport.firstExisting(itemCategoryHm, "Description", "ItemName", "ItemNamePrint", "Name");
        if (hItemName == null) {
            hItemName = hDisplayLevel + 1;
        }

        Map<CategoryItemKey, Deque<DesiredHandyItem>> desiredItemsByKey = new LinkedHashMap<>();
        Map<String, Integer> categoryOrder = new HashMap<>();
        int categoryOrderIndex = 0;
        for (ItemCatalog.Category category : handyCatalog.getCategories()) {
            String categoryCode = safe(category.getCode());
            if (categoryCode.isBlank()) {
                continue;
            }
            categoryOrder.putIfAbsent(categoryCode, categoryOrderIndex);
            categoryOrderIndex += 1;

            int displayLevel = 1;
            for (ItemCatalog.Item item : category.getItems()) {
                String itemCode = safe(item.getItemCode());
                if (itemCode.isBlank()) {
                    continue;
                }
                CategoryItemKey key = new CategoryItemKey(categoryCode, itemCode);
                String itemName = resolveHandyItemName(item);
                desiredItemsByKey
                        .computeIfAbsent(key, ignored -> new ArrayDeque<>())
                        .addLast(new DesiredHandyItem(displayLevel, itemName));
                displayLevel++;
            }
        }

        List<Integer> rowsToDelete = new ArrayList<>();
        for (int r = itemCategoryHm.dataStartRow; r <= itemCategory.getLastRowNum(); r++) {
            Row row = itemCategory.getRow(r);
            if (row == null) continue;

            String categoryCode = safe(u.str(row.getCell(hCategoryCode)));
            String itemCode = safe(u.str(row.getCell(hItemCode)));
            if (categoryCode.isBlank() || itemCode.isBlank()) {
                continue;
            }

            CategoryItemKey key = new CategoryItemKey(categoryCode, itemCode);
            Deque<DesiredHandyItem> desiredItems = desiredItemsByKey.get(key);
            if (desiredItems == null || desiredItems.isEmpty()) {
                rowsToDelete.add(r);
                continue;
            }
            DesiredHandyItem desired = desiredItems.removeFirst();
            setInt(row, hDisplayLevel, desired.displayLevel());
            setStringIfBlank(row, hItemName, desired.itemName());
        }

        for (int i = rowsToDelete.size() - 1; i >= 0; i--) {
            deleteRow(itemCategory, rowsToDelete.get(i));
        }

        Map<String, Integer> firstRowByCategory = new HashMap<>();
        Map<String, Integer> lastRowByCategory = new HashMap<>();
        for (int r = itemCategoryHm.dataStartRow; r <= itemCategory.getLastRowNum(); r++) {
            Row row = itemCategory.getRow(r);
            if (row == null) continue;

            String categoryCode = safe(u.str(row.getCell(hCategoryCode)));
            String itemCode = safe(u.str(row.getCell(hItemCode)));
            if (categoryCode.isBlank() || itemCode.isBlank()) {
                continue;
            }

            firstRowByCategory.putIfAbsent(categoryCode, r);
            lastRowByCategory.put(categoryCode, r);
        }

        for (ItemCatalog.Category category : handyCatalog.getCategories()) {
            String categoryCode = safe(category.getCode());
            if (categoryCode.isBlank()) {
                continue;
            }

            for (ItemCatalog.Item item : category.getItems()) {
                String itemCode = safe(item.getItemCode());
                if (itemCode.isBlank()) {
                    continue;
                }

                CategoryItemKey key = new CategoryItemKey(categoryCode, itemCode);
                Deque<DesiredHandyItem> remaining = desiredItemsByKey.get(key);
                while (remaining != null && !remaining.isEmpty()) {
                    DesiredHandyItem desired = remaining.removeFirst();
                    int insertRowIndex = resolveInsertRowIndex(
                            itemCategory,
                            categoryCode,
                            itemCategoryHm.dataStartRow,
                            firstRowByCategory,
                            lastRowByCategory,
                            categoryOrder
                    );
                    int lastRowBeforeInsert = itemCategory.getLastRowNum();
                    if (insertRowIndex <= lastRowBeforeInsert) {
                        itemCategory.shiftRows(insertRowIndex, lastRowBeforeInsert, 1, true, false);
                        shiftTrackedRows(firstRowByCategory, insertRowIndex);
                        shiftTrackedRows(lastRowByCategory, insertRowIndex);
                    }

                    Row row = itemCategory.createRow(insertRowIndex);
                    setString(row, hCategoryCode, categoryCode);
                    setString(row, hItemCode, itemCode);
                    setInt(row, hDisplayLevel, desired.displayLevel());
                    setString(row, hItemName, desired.itemName());
                    firstRowByCategory.putIfAbsent(categoryCode, insertRowIndex);
                    lastRowByCategory.put(categoryCode, insertRowIndex);
                }
            }
        }
    }

    private static int resolveInsertRowIndex(
            Sheet itemCategory,
            String categoryCode,
            int dataStartRow,
            Map<String, Integer> firstRowByCategory,
            Map<String, Integer> lastRowByCategory,
            Map<String, Integer> categoryOrder
    ) {
        Integer lastRow = lastRowByCategory.get(categoryCode);
        if (lastRow != null) {
            return lastRow + 1;
        }

        int currentOrder = categoryOrder.getOrDefault(categoryCode, Integer.MAX_VALUE);
        int nextCategoryFirstRow = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : firstRowByCategory.entrySet()) {
            int otherOrder = categoryOrder.getOrDefault(entry.getKey(), Integer.MAX_VALUE);
            if (otherOrder <= currentOrder) {
                continue;
            }
            if (entry.getValue() < nextCategoryFirstRow) {
                nextCategoryFirstRow = entry.getValue();
            }
        }
        if (nextCategoryFirstRow != Integer.MAX_VALUE) {
            return nextCategoryFirstRow;
        }
        return Math.max(dataStartRow, itemCategory.getLastRowNum() + 1);
    }

    private static void shiftTrackedRows(Map<String, Integer> rowMap, int fromRowInclusive) {
        for (Map.Entry<String, Integer> entry : rowMap.entrySet()) {
            if (entry.getValue() >= fromRowInclusive) {
                entry.setValue(entry.getValue() + 1);
            }
        }
    }

    private static String resolveHandyItemName(ItemCatalog.Item item) {
        String itemName = safe(item.getItemName());
        if (!itemName.isBlank()) {
            return itemName;
        }
        return safe(item.getItemCode());
    }

    private static String resolveHandyCategoryName(ItemCatalog.Category category) {
        String description = safe(category.getDescription());
        if (!description.isBlank()) {
            return description;
        }
        return safe(category.getCode());
    }

    private static void setStringIfBlank(Row row, Integer colIndex, String value) {
        if (colIndex == null) {
            return;
        }
        Cell cell = row.getCell(colIndex);
        if (cell != null) {
            String current = cell.toString();
            if (current != null && !current.trim().isEmpty()) {
                return;
            }
        }
        setString(row, colIndex, value);
    }

    private static void deleteRow(Sheet sheet, int rowIndex) {
        int lastRow = sheet.getLastRowNum();
        if (rowIndex < 0 || rowIndex > lastRow) {
            return;
        }
        if (rowIndex < lastRow) {
            sheet.shiftRows(rowIndex + 1, lastRow, -1, true, false);
            return;
        }
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            sheet.removeRow(row);
        }
    }

    private static void writeMenuRow(
            Row row,
            int mPage,
            int mCols,
            int mRows,
            int mDesc,
            int mStyle,
            PosConfig.Category category
    ) {
        setInt(row, mPage, category.getPageNumber());
        setInt(row, mCols, category.getCols());
        setInt(row, mRows, category.getRows());
        setString(row, mDesc, category.getName());
        setInt(row, mStyle, category.getStyleKey());
    }

    private static void clearMenuRow(
            Row row,
            int mPage,
            int mCols,
            int mRows,
            int mDesc,
            int mStyle
    ) {
        clearCell(row, mPage);
        clearCell(row, mCols);
        clearCell(row, mRows);
        clearCell(row, mDesc);
        clearCell(row, mStyle);
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
        if (colIndex < 0) {
            return;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value == null ? "" : value);
    }

    private static void setNumericOrString(Row row, int colIndex, String value) {
        String normalized = value.trim().replace(",", "");
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        if (normalized.matches("^\\d+(\\.\\d+)?$")) {
            cell.setCellValue(Double.parseDouble(normalized));
        } else {
            cell.setCellValue(value);
        }
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

    private record IndexedButton(int pageNumber, PosConfig.Button button) {
    }

    private record CategoryItemKey(String categoryCode, String itemCode) {
    }

    private record DesiredHandyItem(int displayLevel, String itemName) {
    }

    private record DesiredHandyCategory(int displayLevel, String description) {
    }
}
