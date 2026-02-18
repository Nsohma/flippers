package com.example.demo.dao;

import com.example.demo.dao.ExcelSupport.ExcelUtil;
import com.example.demo.dao.ExcelSupport.HeaderMap;
import com.example.demo.model.ItemCatalog;
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
    private static final String SHEET_ITEM = "ItemMaster";
    private static final String SHEET_MD_HIERARCHY = "MDHierarchyMaster";
    private static final String SHEET_POS_ITEM = "POSItemMaster";
    private static final String SHEET_CATEGORY_MASTER = "CategoryMaster";
    private static final String SHEET_ITEM_CATEGORY_MASTER = "ItemCategoryMaster";

    @Override
    public PosConfigSource read(InputStream in) throws Exception {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet menu = requireSheet(wb, SHEET_MENU);
            Sheet btn  = requireSheet(wb, SHEET_BUTTON);
            Sheet item = requireSheet(wb, SHEET_ITEM);
            Sheet mdHierarchy = requireSheet(wb, SHEET_MD_HIERARCHY);
            Sheet posItem = requireSheet(wb, SHEET_POS_ITEM);
            Sheet categoryMaster = wb.getSheet(SHEET_CATEGORY_MASTER);
            Sheet itemCategoryMaster = wb.getSheet(SHEET_ITEM_CATEGORY_MASTER);

            ExcelUtil u = new ExcelUtil(wb);

            HeaderMap menuHm = HeaderMap.from(menu, u, "PageNumber");
            HeaderMap btnHm  = HeaderMap.from(btn, u, "PageNumber");
            HeaderMap itemHm = HeaderMap.from(item, u, "ItemCode", "UnitPrice");
            HeaderMap mdHm = HeaderMap.from(mdHierarchy, u, "MDHierarchyCode");
            HeaderMap posHm = HeaderMap.from(posItem, u, "MDHierarchyCode", "ItemCode");

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

            int iCode = itemHm.require("ItemCode");
            Integer iName = itemHm.get("ItemName");
            Integer iNamePrint = itemHm.get("ItemNamePrint");
            int iUnitPrice = itemHm.require("UnitPrice");

            int hCode = mdHm.require("MDHierarchyCode");
            int hDesc = mdHm.require("Description");

            int pCode = posHm.require("MDHierarchyCode");
            int pItemCode = posHm.require("ItemCode");

            Map<String, ItemInfo> itemInfoByItemCode = new LinkedHashMap<>();
            Map<String, String> unitPriceByItemCode = new HashMap<>();
            for (int r = itemHm.dataStartRow; r <= item.getLastRowNum(); r++) {
                Row row = item.getRow(r);
                if (row == null) continue;

                String itemCode = nonNull(u.str(row.getCell(iCode)));
                if (isBlank(itemCode)) continue;

                String itemName = "";
                if (iName != null) {
                    itemName = nonNull(u.str(row.getCell(iName)));
                }
                if (isBlank(itemName) && iNamePrint != null) {
                    itemName = nonNull(u.str(row.getCell(iNamePrint)));
                }
                String unitPrice = nonNull(u.str(row.getCell(iUnitPrice)));
                itemInfoByItemCode.put(itemCode, new ItemInfo(itemName, unitPrice));
                unitPriceByItemCode.put(itemCode, unitPrice);
            }

            Map<String, String> categoryDescriptionByCode = new LinkedHashMap<>();
            for (int r = mdHm.dataStartRow; r <= mdHierarchy.getLastRowNum(); r++) {
                Row row = mdHierarchy.getRow(r);
                if (row == null) continue;

                String categoryCode = nonNull(u.str(row.getCell(hCode)));
                if (isBlank(categoryCode)) continue;

                String categoryDescription = nonNull(u.str(row.getCell(hDesc)));
                categoryDescriptionByCode.put(categoryCode, categoryDescription);
            }

            Map<String, LinkedHashSet<String>> itemCodesByCategoryCode = new LinkedHashMap<>();
            for (int r = posHm.dataStartRow; r <= posItem.getLastRowNum(); r++) {
                Row row = posItem.getRow(r);
                if (row == null) continue;

                String categoryCode = nonNull(u.str(row.getCell(pCode)));
                String itemCode = nonNull(u.str(row.getCell(pItemCode)));
                if (isBlank(categoryCode) || isBlank(itemCode)) continue;

                itemCodesByCategoryCode
                        .computeIfAbsent(categoryCode, k -> new LinkedHashSet<>())
                        .add(itemCode);
            }

            List<ItemCatalog.Category> itemCategories = new ArrayList<>();
            Set<String> appendedCategoryCodes = new HashSet<>();
            for (Map.Entry<String, String> entry : categoryDescriptionByCode.entrySet()) {
                String categoryCode = entry.getKey();
                LinkedHashSet<String> itemCodes = itemCodesByCategoryCode.get(categoryCode);
                if (itemCodes == null || itemCodes.isEmpty()) continue;

                itemCategories.add(toItemCategory(
                        categoryCode,
                        entry.getValue(),
                        itemCodes,
                        itemInfoByItemCode
                ));
                appendedCategoryCodes.add(categoryCode);
            }
            for (Map.Entry<String, LinkedHashSet<String>> entry : itemCodesByCategoryCode.entrySet()) {
                String categoryCode = entry.getKey();
                if (appendedCategoryCodes.contains(categoryCode)) continue;

                itemCategories.add(toItemCategory(
                        categoryCode,
                        categoryCode,
                        entry.getValue(),
                        itemInfoByItemCode
                ));
            }
            ItemCatalog itemCatalog = new ItemCatalog(itemCategories);

            ItemCatalog handyCatalog = ItemCatalog.empty();
            if (categoryMaster != null && itemCategoryMaster != null) {
                HeaderMap categoryHm = HeaderMap.from(categoryMaster, u, "CategoryCode", "DisplayLevel");
                HeaderMap itemCategoryHm = HeaderMap.from(itemCategoryMaster, u, "CategoryCode", "ItemCode");

                int cCode = categoryHm.require("CategoryCode");
                int cDisplayLevel = categoryHm.require("DisplayLevel");
                Integer cDescription = ExcelSupport.firstExisting(
                        categoryHm,
                        "Description",
                        "CategoryName",
                        "CategoryDescription"
                );
                int icCategoryCode = itemCategoryHm.require("CategoryCode");
                int icItemCode = itemCategoryHm.require("ItemCode");
                int icDisplayLevel = itemCategoryHm.require("DisplayLevel");

                Map<String, HandyCategoryMeta> handyCategoryByCode = new LinkedHashMap<>();
                for (int r = categoryHm.dataStartRow; r <= categoryMaster.getLastRowNum(); r++) {
                    Row row = categoryMaster.getRow(r);
                    if (row == null) continue;

                    String categoryCode = nonNull(u.str(row.getCell(cCode)));
                    if (isBlank(categoryCode)) continue;

                    String displayLevelRaw = nonNull(u.str(row.getCell(cDisplayLevel)));
                    if (isBlank(displayLevelRaw)) continue;
                    int displayLevel = parseIntStrict(displayLevelRaw, "CategoryMaster.DisplayLevel");

                    String description = cDescription == null
                            ? categoryCode
                            : nonNull(u.str(row.getCell(cDescription)));
                    if (isBlank(description)) {
                        description = categoryCode;
                    }

                    HandyCategoryMeta current = handyCategoryByCode.get(categoryCode);
                    if (current == null || displayLevel < current.displayLevel()) {
                        handyCategoryByCode.put(
                                categoryCode,
                                new HandyCategoryMeta(categoryCode, description, displayLevel)
                        );
                    }
                }

                Map<String, List<HandyItemRef>> handyItemsByCategoryCode = new LinkedHashMap<>();
                long itemSequence = 0L;
                for (int r = itemCategoryHm.dataStartRow; r <= itemCategoryMaster.getLastRowNum(); r++) {
                    Row row = itemCategoryMaster.getRow(r);
                    if (row == null) continue;

                    String categoryCode = nonNull(u.str(row.getCell(icCategoryCode)));
                    String itemCode = nonNull(u.str(row.getCell(icItemCode)));
                    if (isBlank(categoryCode) || isBlank(itemCode)) continue;

                    int displayLevel = parseIntStrict(
                            nonNull(u.str(row.getCell(icDisplayLevel))),
                            "ItemCategoryMaster.DisplayLevel"
                    );

                    handyItemsByCategoryCode
                            .computeIfAbsent(categoryCode, k -> new ArrayList<>())
                            .add(new HandyItemRef(itemCode, displayLevel, itemSequence));
                    itemSequence += 1;
                }

                List<HandyCategoryMeta> sortedHandyCategories = new ArrayList<>(handyCategoryByCode.values());
                sortedHandyCategories.sort(
                        Comparator.comparingInt(HandyCategoryMeta::displayLevel)
                                .thenComparing(HandyCategoryMeta::categoryCode)
                );

                for (String categoryCode : handyItemsByCategoryCode.keySet()) {
                    if (handyCategoryByCode.containsKey(categoryCode)) {
                        continue;
                    }
                    sortedHandyCategories.add(new HandyCategoryMeta(categoryCode, categoryCode, Integer.MAX_VALUE));
                }

                List<ItemCatalog.Category> handyCategories = new ArrayList<>();
                for (HandyCategoryMeta meta : sortedHandyCategories) {
                    List<HandyItemRef> handyItems = new ArrayList<>(
                            handyItemsByCategoryCode.getOrDefault(meta.categoryCode(), List.of())
                    );
                    handyItems.sort(
                            Comparator.comparingInt(HandyItemRef::displayLevel)
                                    .thenComparingLong(HandyItemRef::sequence)
                    );
                    List<String> itemCodes = handyItems.stream()
                            .map(HandyItemRef::itemCode)
                            .toList();
                    handyCategories.add(
                            toItemCategory(
                                    meta.categoryCode(),
                                    meta.description(),
                                    itemCodes,
                                    itemInfoByItemCode
                            )
                    );
                }
                handyCatalog = new ItemCatalog(handyCategories);
            }

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
                String unitPrice = unitPriceByItemCode.getOrDefault(itemCode, "");
                String buttonId = buttonRowId(r);

                pageButtons.add(
                        new PosConfigSource.PageButton(
                                page,
                                new PosConfig.Button(col, rowNo, desc, style, itemCode, unitPrice, buttonId)
                        )
                );
            }

            return new PosConfigSource(categories, pageButtons, itemCatalog, handyCatalog);
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
    private static String buttonRowId(int zeroBasedRowIndex) {
        return SHEET_BUTTON + "#R" + (zeroBasedRowIndex + 1);
    }

    private static ItemCatalog.Category toItemCategory(
            String categoryCode,
            String categoryDescription,
            Collection<String> itemCodes,
            Map<String, ItemInfo> itemInfoByItemCode
    ) {
        List<ItemCatalog.Item> items = new ArrayList<>();
        for (String itemCode : itemCodes) {
            ItemInfo info = itemInfoByItemCode.get(itemCode);
            String itemName = info == null ? itemCode : normalizeItemName(info.itemName(), itemCode);
            String unitPrice = info == null ? "" : info.unitPrice();
            items.add(new ItemCatalog.Item(itemCode, itemName, unitPrice));
        }
        String normalizedDescription = isBlank(categoryDescription) ? categoryCode : categoryDescription;
        return new ItemCatalog.Category(categoryCode, normalizedDescription, items);
    }

    private static String normalizeItemName(String itemName, String fallback) {
        return isBlank(itemName) ? fallback : itemName;
    }

    private static int parseIntStrict(String s, String field) {
        if (s == null) throw new IllegalArgumentException("Missing value: " + field);
        String t = s.trim();
        if (t.matches("^-?\\d+\\.0$")) t = t.substring(0, t.length() - 2);
        if (!t.matches("^-?\\d+$")) throw new IllegalArgumentException("Not an int (" + field + "): " + s);
        return Integer.parseInt(t);
    }

    private record ItemInfo(String itemName, String unitPrice) {
    }

    private record HandyCategoryMeta(String categoryCode, String description, int displayLevel) {
    }

    private record HandyItemRef(String itemCode, int displayLevel, long sequence) {
    }
}
