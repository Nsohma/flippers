package com.example.demo.model;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PosDraft implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int HISTORY_LIMIT = 100;

    private final String draftId;
    private final PosConfig config;
    private final byte[] originalExcelBytes;
    private final ItemCatalog itemCatalog;
    private final ItemCatalog handyCatalog;
    private final String initialAction;
    private final String initialTimestamp;
    private final List<ChangeRecord> changes;
    private final int historyIndex;

    public PosDraft(String draftId, PosConfig config, byte[] originalExcelBytes) {
        this(draftId, config, originalExcelBytes, null, null, "インポート");
    }

    public PosDraft(String draftId, PosConfig config, byte[] originalExcelBytes, ItemCatalog itemCatalog) {
        this(draftId, config, originalExcelBytes, itemCatalog, null, "インポート");
    }

    public PosDraft(
            String draftId,
            PosConfig config,
            byte[] originalExcelBytes,
            ItemCatalog itemCatalog,
            ItemCatalog handyCatalog
    ) {
        this(draftId, config, originalExcelBytes, itemCatalog, handyCatalog, "インポート");
    }

    public PosDraft(
            String draftId,
            PosConfig config,
            byte[] originalExcelBytes,
            ItemCatalog itemCatalog,
            ItemCatalog handyCatalog,
            String initialAction
    ) {
        this(
                draftId,
                config,
                originalExcelBytes,
                itemCatalog,
                handyCatalog,
                initialAction,
                OffsetDateTime.now().toString(),
                null,
                -1
        );
    }

    private PosDraft(
            String draftId,
            PosConfig config,
            byte[] originalExcelBytes,
            ItemCatalog itemCatalog,
            ItemCatalog handyCatalog,
            String initialAction,
            String initialTimestamp,
            List<ChangeRecord> changes,
            int historyIndex
    ) {
        this.draftId = Objects.requireNonNull(draftId);
        this.config = Objects.requireNonNull(config);
        this.originalExcelBytes = Objects.requireNonNull(originalExcelBytes).clone();
        this.itemCatalog = itemCatalog;
        this.handyCatalog = handyCatalog;
        this.initialAction = normalizeInitialAction(initialAction);
        this.initialTimestamp = normalizeTimestamp(initialTimestamp);
        this.changes = normalizeChanges(changes);
        this.historyIndex = normalizeHistoryIndex(historyIndex, this.changes.size());
    }

    public String getDraftId() {
        return draftId;
    }

    public PosConfig getConfig() {
        return config;
    }

    public byte[] getOriginalExcelBytes() {
        return originalExcelBytes.clone();
    }

    public ItemCatalog getItemCatalogOrNull() {
        return itemCatalog;
    }

    public ItemCatalog getHandyCatalogOrNull() {
        return handyCatalog;
    }

    public List<HistoryEntry> getHistoryEntries() {
        List<HistoryEntry> entries = new ArrayList<>(changes.size() + 1);
        entries.add(new HistoryEntry(initialAction, initialTimestamp));
        for (ChangeRecord changeRecord : changes) {
            entries.add(changeRecord.getEntry());
        }
        return List.copyOf(entries);
    }

    public int getHistoryIndex() {
        return historyIndex;
    }

    public boolean canUndo() {
        return historyIndex > 0;
    }

    public boolean canRedo() {
        return historyIndex < changes.size();
    }

    public PosDraft applyNewConfig(PosConfig nextConfig) {
        return applyNewConfig(nextConfig, "編集");
    }

    public PosDraft applyNewConfig(PosConfig nextConfig, String action) {
        Objects.requireNonNull(nextConfig);
        return applyChange(new SnapshotReplaceChange(config, nextConfig), action);
    }

    public PosDraft applyChange(Change change, String action) {
        Objects.requireNonNull(change);

        PosConfig nextConfig = change.apply(config);
        ItemCatalog nextItemCatalog = change.applyItemCatalog(itemCatalog);
        ItemCatalog nextHandyCatalog = change.applyHandyCatalog(handyCatalog);
        List<ChangeRecord> nextChanges = new ArrayList<>(changes.subList(0, historyIndex));
        nextChanges.add(new ChangeRecord(change, HistoryEntry.of(action)));

        int nextHistoryIndex = nextChanges.size();
        int removeCount = Math.max(0, (nextChanges.size() + 1) - HISTORY_LIMIT);
        if (removeCount > 0) {
            nextChanges = new ArrayList<>(nextChanges.subList(removeCount, nextChanges.size()));
            nextHistoryIndex -= removeCount;
        }

        return new PosDraft(
                draftId,
                nextConfig,
                originalExcelBytes,
                nextItemCatalog,
                nextHandyCatalog,
                initialAction,
                initialTimestamp,
                nextChanges,
                nextHistoryIndex
        );
    }

    public PosDraft undo() {
        if (!canUndo()) {
            throw new IllegalArgumentException("undo not available");
        }
        ChangeRecord changeRecord = changes.get(historyIndex - 1);
        PosConfig previousConfig = changeRecord.getChange().undo(config);
        ItemCatalog previousItemCatalog = changeRecord.getChange().undoItemCatalog(itemCatalog);
        ItemCatalog previousHandyCatalog = changeRecord.getChange().undoHandyCatalog(handyCatalog);
        return new PosDraft(
                draftId,
                previousConfig,
                originalExcelBytes,
                previousItemCatalog,
                previousHandyCatalog,
                initialAction,
                initialTimestamp,
                changes,
                historyIndex - 1
        );
    }

    public PosDraft redo() {
        if (!canRedo()) {
            throw new IllegalArgumentException("redo not available");
        }
        ChangeRecord changeRecord = changes.get(historyIndex);
        PosConfig nextConfig = changeRecord.getChange().apply(config);
        ItemCatalog nextItemCatalog = changeRecord.getChange().applyItemCatalog(itemCatalog);
        ItemCatalog nextHandyCatalog = changeRecord.getChange().applyHandyCatalog(handyCatalog);
        return new PosDraft(
                draftId,
                nextConfig,
                originalExcelBytes,
                nextItemCatalog,
                nextHandyCatalog,
                initialAction,
                initialTimestamp,
                changes,
                historyIndex + 1
        );
    }

    public PosDraft jumpToHistoryIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex > changes.size()) {
            throw new IllegalArgumentException("history index out of range: " + targetIndex);
        }
        if (targetIndex == historyIndex) {
            return this;
        }

        PosDraft current = this;
        while (current.historyIndex < targetIndex) {
            current = current.redo();
        }
        while (current.historyIndex > targetIndex) {
            current = current.undo();
        }
        return current;
    }

    public PosDraft clearHistory() {
        return clearHistory("履歴削除");
    }

    public PosDraft clearHistory(String action) {
        return new PosDraft(
                draftId,
                config,
                originalExcelBytes,
                itemCatalog,
                handyCatalog,
                action,
                OffsetDateTime.now().toString(),
                List.of(),
                -1
        );
    }

    public PosDraft withItemCatalog(ItemCatalog nextItemCatalog) {
        if (nextItemCatalog == itemCatalog) {
            return this;
        }
        return new PosDraft(
                draftId,
                config,
                originalExcelBytes,
                nextItemCatalog,
                handyCatalog,
                initialAction,
                initialTimestamp,
                changes,
                historyIndex
        );
    }

    public PosDraft withHandyCatalog(ItemCatalog nextHandyCatalog) {
        if (nextHandyCatalog == handyCatalog) {
            return this;
        }
        return new PosDraft(
                draftId,
                config,
                originalExcelBytes,
                itemCatalog,
                nextHandyCatalog,
                initialAction,
                initialTimestamp,
                changes,
                historyIndex
        );
    }

    private Object readResolve() throws ObjectStreamException {
        return new PosDraft(
                draftId,
                config,
                originalExcelBytes,
                itemCatalog,
                handyCatalog,
                initialAction,
                initialTimestamp,
                changes,
                historyIndex
        );
    }

    private static List<ChangeRecord> normalizeChanges(List<ChangeRecord> changes) {
        if (changes == null || changes.isEmpty()) {
            return List.of();
        }
        return List.copyOf(changes);
    }

    private static int normalizeHistoryIndex(int historyIndex, int changeCount) {
        if (historyIndex < 0 || historyIndex > changeCount) {
            return changeCount;
        }
        return historyIndex;
    }

    private static String normalizeInitialAction(String action) {
        if (action == null || action.isBlank()) {
            return "インポート";
        }
        return action;
    }

    private static String normalizeTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return OffsetDateTime.now().toString();
        }
        return timestamp;
    }

    public interface Change extends Serializable {
        PosConfig apply(PosConfig config);

        PosConfig undo(PosConfig config);

        default ItemCatalog applyItemCatalog(ItemCatalog itemCatalog) {
            return itemCatalog;
        }

        default ItemCatalog undoItemCatalog(ItemCatalog itemCatalog) {
            return itemCatalog;
        }

        default ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return handyCatalog;
        }

        default ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return handyCatalog;
        }
    }

    public static class SwapButtonsChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int pageNumber;
        private final int fromCol;
        private final int fromRow;
        private final int toCol;
        private final int toRow;

        public SwapButtonsChange(int pageNumber, int fromCol, int fromRow, int toCol, int toRow) {
            this.pageNumber = pageNumber;
            this.fromCol = fromCol;
            this.fromRow = fromRow;
            this.toCol = toCol;
            this.toRow = toRow;
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config.swapButtons(pageNumber, fromCol, fromRow, toCol, toRow);
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.swapButtons(pageNumber, toCol, toRow, fromCol, fromRow);
        }
    }

    public static class AddButtonChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int pageNumber;
        private final PosConfig.Button button;

        public AddButtonChange(int pageNumber, PosConfig.Button button) {
            this.pageNumber = pageNumber;
            this.button = Objects.requireNonNull(button);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config.addButton(
                    pageNumber,
                    button.getCol(),
                    button.getRow(),
                    button.getLabel(),
                    button.getStyleKey(),
                    button.getItemCode(),
                    button.getUnitPrice(),
                    button.getButtonId()
            );
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.deleteButton(pageNumber, button.getButtonId());
        }
    }

    public static class DeleteButtonChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int pageNumber;
        private final PosConfig.Button button;

        public DeleteButtonChange(int pageNumber, PosConfig.Button button) {
            this.pageNumber = pageNumber;
            this.button = Objects.requireNonNull(button);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config.deleteButton(pageNumber, button.getButtonId());
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.addButton(
                    pageNumber,
                    button.getCol(),
                    button.getRow(),
                    button.getLabel(),
                    button.getStyleKey(),
                    button.getItemCode(),
                    button.getUnitPrice(),
                    button.getButtonId()
            );
        }
    }

    public static class UpdateUnitPriceChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int pageNumber;
        private final String buttonId;
        private final String beforeUnitPrice;
        private final String afterUnitPrice;

        public UpdateUnitPriceChange(int pageNumber, String buttonId, String beforeUnitPrice, String afterUnitPrice) {
            this.pageNumber = pageNumber;
            this.buttonId = Objects.requireNonNull(buttonId);
            this.beforeUnitPrice = beforeUnitPrice == null ? "" : beforeUnitPrice;
            this.afterUnitPrice = afterUnitPrice == null ? "" : afterUnitPrice;
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config.updateUnitPrice(pageNumber, buttonId, afterUnitPrice);
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.updateUnitPrice(pageNumber, buttonId, beforeUnitPrice);
        }
    }

    public static class AddCategoryChange implements Change {
        private static final long serialVersionUID = 1L;

        private final PosConfig.Category category;

        public AddCategoryChange(PosConfig.Category category) {
            this.category = Objects.requireNonNull(category);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            PosConfig.Page emptyPage = new PosConfig.Page(
                    category.getPageNumber(),
                    category.getCols(),
                    category.getRows(),
                    List.of()
            );
            return config.restoreCategory(category, emptyPage);
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.deleteCategory(category.getPageNumber());
        }
    }

    public static class DeleteCategoryChange implements Change {
        private static final long serialVersionUID = 1L;

        private final PosConfig.Category category;
        private final PosConfig.Page page;

        public DeleteCategoryChange(PosConfig.Category category, PosConfig.Page page) {
            this.category = Objects.requireNonNull(category);
            this.page = Objects.requireNonNull(page);
            if (category.getPageNumber() != page.getPageNumber()) {
                throw new IllegalArgumentException("category/page mismatch");
            }
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config.deleteCategory(category.getPageNumber());
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.restoreCategory(category, page);
        }
    }

    public static class UpdateCategoryGridChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int pageNumber;
        private final int fromCols;
        private final int fromRows;
        private final int toCols;
        private final int toRows;

        public UpdateCategoryGridChange(int pageNumber, int fromCols, int fromRows, int toCols, int toRows) {
            this.pageNumber = pageNumber;
            this.fromCols = fromCols;
            this.fromRows = fromRows;
            this.toCols = toCols;
            this.toRows = toRows;
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config.updateCategoryGrid(pageNumber, toCols, toRows);
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.updateCategoryGrid(pageNumber, fromCols, fromRows);
        }
    }

    public static class SwapCategoriesChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int fromPageNumber;
        private final int toPageNumber;

        public SwapCategoriesChange(int fromPageNumber, int toPageNumber) {
            this.fromPageNumber = fromPageNumber;
            this.toPageNumber = toPageNumber;
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config.swapCategories(fromPageNumber, toPageNumber);
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config.swapCategories(fromPageNumber, toPageNumber);
        }
    }

    public static class ReorderHandyItemsChange implements Change {
        private static final long serialVersionUID = 1L;

        private final String categoryCode;
        private final int fromIndex;
        private final int toIndex;

        public ReorderHandyItemsChange(String categoryCode, int fromIndex, int toIndex) {
            this.categoryCode = categoryCode == null ? "" : categoryCode.trim();
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config;
        }

        @Override
        public ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return reorderHandyItemsByIndex(handyCatalog, categoryCode, fromIndex, toIndex);
        }

        @Override
        public ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return reorderHandyItemsByIndex(handyCatalog, categoryCode, toIndex, fromIndex);
        }
    }

    public static class DeleteHandyItemChange implements Change {
        private static final long serialVersionUID = 1L;

        private final String categoryCode;
        private final int itemIndex;
        private final ItemCatalog.Item deletedItem;

        public DeleteHandyItemChange(String categoryCode, int itemIndex, ItemCatalog.Item deletedItem) {
            this.categoryCode = categoryCode == null ? "" : categoryCode.trim();
            this.itemIndex = itemIndex;
            this.deletedItem = Objects.requireNonNull(deletedItem);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config;
        }

        @Override
        public ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return deleteHandyItemByIndex(handyCatalog, categoryCode, itemIndex);
        }

        @Override
        public ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return insertHandyItemAt(handyCatalog, categoryCode, itemIndex, deletedItem);
        }
    }

    public static class AddHandyItemChange implements Change {
        private static final long serialVersionUID = 1L;

        private final String categoryCode;
        private final int itemIndex;
        private final ItemCatalog.Item addedItem;

        public AddHandyItemChange(String categoryCode, int itemIndex, ItemCatalog.Item addedItem) {
            this.categoryCode = categoryCode == null ? "" : categoryCode.trim();
            this.itemIndex = itemIndex;
            this.addedItem = Objects.requireNonNull(addedItem);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config;
        }

        @Override
        public ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return insertHandyItemAt(handyCatalog, categoryCode, itemIndex, addedItem);
        }

        @Override
        public ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return deleteHandyItemByIndex(handyCatalog, categoryCode, itemIndex);
        }
    }

    public static class AddHandyCategoryChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int categoryIndex;
        private final ItemCatalog.Category category;

        public AddHandyCategoryChange(int categoryIndex, ItemCatalog.Category category) {
            this.categoryIndex = categoryIndex;
            this.category = Objects.requireNonNull(category);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config;
        }

        @Override
        public ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return insertHandyCategoryAt(handyCatalog, categoryIndex, category);
        }

        @Override
        public ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return deleteHandyCategoryByCode(handyCatalog, category.getCode());
        }
    }

    public static class DeleteHandyCategoryChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int categoryIndex;
        private final ItemCatalog.Category category;

        public DeleteHandyCategoryChange(int categoryIndex, ItemCatalog.Category category) {
            this.categoryIndex = categoryIndex;
            this.category = Objects.requireNonNull(category);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config;
        }

        @Override
        public ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return deleteHandyCategoryByIndex(handyCatalog, categoryIndex, category.getCode());
        }

        @Override
        public ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return insertHandyCategoryAt(handyCatalog, categoryIndex, category);
        }
    }

    public static class ReorderHandyCategoriesChange implements Change {
        private static final long serialVersionUID = 1L;

        private final int fromIndex;
        private final int toIndex;

        public ReorderHandyCategoriesChange(int fromIndex, int toIndex) {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config;
        }

        @Override
        public ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return reorderHandyCategoriesByIndex(handyCatalog, fromIndex, toIndex);
        }

        @Override
        public ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return reorderHandyCategoriesByIndex(handyCatalog, toIndex, fromIndex);
        }
    }

    public static class SwapHandyCategoriesChange implements Change {
        private static final long serialVersionUID = 1L;

        private final String fromCategoryCode;
        private final String toCategoryCode;

        public SwapHandyCategoriesChange(String fromCategoryCode, String toCategoryCode) {
            this.fromCategoryCode = fromCategoryCode == null ? "" : fromCategoryCode.trim();
            this.toCategoryCode = toCategoryCode == null ? "" : toCategoryCode.trim();
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return config;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return config;
        }

        @Override
        public ItemCatalog applyHandyCatalog(ItemCatalog handyCatalog) {
            return swapHandyCategoriesByCode(handyCatalog, fromCategoryCode, toCategoryCode);
        }

        @Override
        public ItemCatalog undoHandyCatalog(ItemCatalog handyCatalog) {
            return swapHandyCategoriesByCode(handyCatalog, fromCategoryCode, toCategoryCode);
        }
    }

    private static ItemCatalog reorderHandyItemsByIndex(ItemCatalog handyCatalog, String categoryCode, int fromIndex, int toIndex) {
        HandyCategoryContext context = requireHandyCategory(handyCatalog, categoryCode);
        ItemCatalog.Category category = context.category();
        List<ItemCatalog.Item> items = new ArrayList<>(category.getItems());
        if (fromIndex < 0 || fromIndex >= items.size()) {
            throw new IllegalArgumentException("fromIndex out of range: " + fromIndex);
        }
        if (toIndex < 0 || toIndex >= items.size()) {
            throw new IllegalArgumentException("toIndex out of range: " + toIndex);
        }
        if (fromIndex == toIndex) {
            return handyCatalog;
        }

        ItemCatalog.Item moved = items.remove(fromIndex);
        items.add(toIndex, moved);
        return replaceHandyCategoryItems(context, items);
    }

    private static ItemCatalog deleteHandyItemByIndex(ItemCatalog handyCatalog, String categoryCode, int itemIndex) {
        HandyCategoryContext context = requireHandyCategory(handyCatalog, categoryCode);
        ItemCatalog.Category category = context.category();
        List<ItemCatalog.Item> items = new ArrayList<>(category.getItems());
        if (itemIndex < 0 || itemIndex >= items.size()) {
            throw new IllegalArgumentException("itemIndex out of range: " + itemIndex);
        }

        items.remove(itemIndex);
        return replaceHandyCategoryItems(context, items);
    }

    private static ItemCatalog insertHandyItemAt(
            ItemCatalog handyCatalog,
            String categoryCode,
            int itemIndex,
            ItemCatalog.Item item
    ) {
        HandyCategoryContext context = requireHandyCategory(handyCatalog, categoryCode);
        ItemCatalog.Category category = context.category();
        List<ItemCatalog.Item> items = new ArrayList<>(category.getItems());
        if (itemIndex < 0 || itemIndex > items.size()) {
            throw new IllegalArgumentException("itemIndex out of range: " + itemIndex);
        }

        items.add(itemIndex, item);
        return replaceHandyCategoryItems(context, items);
    }

    private static ItemCatalog insertHandyCategoryAt(
            ItemCatalog handyCatalog,
            int categoryIndex,
            ItemCatalog.Category category
    ) {
        if (handyCatalog == null) {
            throw new IllegalStateException("handy catalog is not loaded");
        }
        if (categoryIndex < 0 || categoryIndex > handyCatalog.getCategories().size()) {
            throw new IllegalArgumentException("categoryIndex out of range: " + categoryIndex);
        }
        if (category.getCode() == null || category.getCode().isBlank()) {
            throw new IllegalArgumentException("categoryCode is required");
        }
        if (handyCatalog.findCategory(category.getCode()) != null) {
            throw new IllegalArgumentException("handy category already exists: " + category.getCode());
        }

        List<ItemCatalog.Category> categories = new ArrayList<>(handyCatalog.getCategories());
        categories.add(categoryIndex, category);
        return new ItemCatalog(categories);
    }

    private static ItemCatalog deleteHandyCategoryByIndex(
            ItemCatalog handyCatalog,
            int categoryIndex,
            String expectedCategoryCode
    ) {
        if (handyCatalog == null) {
            throw new IllegalStateException("handy catalog is not loaded");
        }
        List<ItemCatalog.Category> categories = new ArrayList<>(handyCatalog.getCategories());
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            throw new IllegalArgumentException("categoryIndex out of range: " + categoryIndex);
        }
        ItemCatalog.Category target = categories.get(categoryIndex);
        if (expectedCategoryCode != null && !expectedCategoryCode.isBlank()
                && !expectedCategoryCode.equals(target.getCode())) {
            throw new IllegalArgumentException("handy category mismatch: " + expectedCategoryCode);
        }
        categories.remove(categoryIndex);
        return new ItemCatalog(categories);
    }

    private static ItemCatalog deleteHandyCategoryByCode(ItemCatalog handyCatalog, String categoryCode) {
        if (handyCatalog == null) {
            throw new IllegalStateException("handy catalog is not loaded");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new IllegalArgumentException("categoryCode is required");
        }

        List<ItemCatalog.Category> categories = new ArrayList<>(handyCatalog.getCategories());
        int categoryIndex = findHandyCategoryIndex(categories, categoryCode);
        categories.remove(categoryIndex);
        return new ItemCatalog(categories);
    }

    private static ItemCatalog reorderHandyCategoriesByIndex(
            ItemCatalog handyCatalog,
            int fromIndex,
            int toIndex
    ) {
        if (handyCatalog == null) {
            throw new IllegalStateException("handy catalog is not loaded");
        }

        List<ItemCatalog.Category> categories = new ArrayList<>(handyCatalog.getCategories());
        if (fromIndex < 0 || fromIndex >= categories.size()) {
            throw new IllegalArgumentException("fromIndex out of range: " + fromIndex);
        }
        if (toIndex < 0 || toIndex >= categories.size()) {
            throw new IllegalArgumentException("toIndex out of range: " + toIndex);
        }
        if (fromIndex == toIndex) {
            return handyCatalog;
        }

        ItemCatalog.Category moved = categories.remove(fromIndex);
        categories.add(toIndex, moved);
        return new ItemCatalog(categories);
    }

    private static ItemCatalog swapHandyCategoriesByCode(
            ItemCatalog handyCatalog,
            String fromCategoryCode,
            String toCategoryCode
    ) {
        if (handyCatalog == null) {
            throw new IllegalStateException("handy catalog is not loaded");
        }
        if (fromCategoryCode == null || fromCategoryCode.isBlank()) {
            throw new IllegalArgumentException("fromCategoryCode is required");
        }
        if (toCategoryCode == null || toCategoryCode.isBlank()) {
            throw new IllegalArgumentException("toCategoryCode is required");
        }

        List<ItemCatalog.Category> categories = new ArrayList<>(handyCatalog.getCategories());
        int fromIndex = findHandyCategoryIndex(categories, fromCategoryCode);
        int toIndex = findHandyCategoryIndex(categories, toCategoryCode);
        if (fromIndex == toIndex) {
            return handyCatalog;
        }
        ItemCatalog.Category fromCategory = categories.get(fromIndex);
        categories.set(fromIndex, categories.get(toIndex));
        categories.set(toIndex, fromCategory);
        return new ItemCatalog(categories);
    }

    private static HandyCategoryContext requireHandyCategory(ItemCatalog handyCatalog, String categoryCode) {
        if (handyCatalog == null) {
            throw new IllegalStateException("handy catalog is not loaded");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new IllegalArgumentException("categoryCode is required");
        }

        List<ItemCatalog.Category> categories = new ArrayList<>(handyCatalog.getCategories());
        int categoryIndex = findHandyCategoryIndex(categories, categoryCode);
        return new HandyCategoryContext(categories, categoryIndex, categories.get(categoryIndex));
    }

    private static int findHandyCategoryIndex(List<ItemCatalog.Category> categories, String categoryCode) {
        for (int i = 0; i < categories.size(); i++) {
            if (categoryCode.equals(categories.get(i).getCode())) {
                return i;
            }
        }
        throw new IllegalArgumentException("handy category not found: " + categoryCode);
    }

    private static ItemCatalog replaceHandyCategoryItems(HandyCategoryContext context, List<ItemCatalog.Item> items) {
        ItemCatalog.Category category = context.category();
        context.categories().set(
                context.categoryIndex(),
                new ItemCatalog.Category(category.getCode(), category.getDescription(), items)
        );
        return new ItemCatalog(context.categories());
    }

    private static class SnapshotReplaceChange implements Change {
        private static final long serialVersionUID = 1L;

        private final PosConfig beforeConfig;
        private final PosConfig afterConfig;

        private SnapshotReplaceChange(PosConfig beforeConfig, PosConfig afterConfig) {
            this.beforeConfig = Objects.requireNonNull(beforeConfig);
            this.afterConfig = Objects.requireNonNull(afterConfig);
        }

        @Override
        public PosConfig apply(PosConfig config) {
            return afterConfig;
        }

        @Override
        public PosConfig undo(PosConfig config) {
            return beforeConfig;
        }
    }

    private static class ChangeRecord implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Change change;
        private final HistoryEntry entry;

        private ChangeRecord(Change change, HistoryEntry entry) {
            this.change = Objects.requireNonNull(change);
            this.entry = Objects.requireNonNull(entry);
        }

        private Change getChange() {
            return change;
        }

        private HistoryEntry getEntry() {
            return entry;
        }
    }

    private record HandyCategoryContext(
            List<ItemCatalog.Category> categories,
            int categoryIndex,
            ItemCatalog.Category category
    ) {
    }

    public static class HistoryEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String action;
        private final String timestamp;

        public HistoryEntry(String action, String timestamp) {
            this.action = action == null || action.isBlank() ? "編集" : action;
            this.timestamp = timestamp == null ? "" : timestamp;
        }

        public String getAction() {
            return action;
        }

        public String getTimestamp() {
            return timestamp;
        }

        static HistoryEntry of(String action) {
            return new HistoryEntry(action, OffsetDateTime.now().toString());
        }
    }
}
