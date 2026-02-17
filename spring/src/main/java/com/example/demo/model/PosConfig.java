package com.example.demo.model;

import java.util.List;
import java.util.Map;

public class PosConfig {
    private final List<Category> categories;               // 上段
    private final Map<Integer, Page> pagesByPageNumber;    // 下段（pageNumberごと）

    public PosConfig(List<Category> categories, Map<Integer, Page> pagesByPageNumber) {
        this.categories = categories;
        this.pagesByPageNumber = pagesByPageNumber;
    }

    public List<Category> getCategories() { return categories; }
    public Map<Integer, Page> getPagesByPageNumber() { return pagesByPageNumber; }

    public Category firstCategoryOrNull() {
        return categories.isEmpty() ? null : categories.get(0);
    }

    public Page getPage(int pageNumber) {
        return pagesByPageNumber.get(pageNumber);
    }

    // ---- Value-like domain classes ----
    public static class Category {
        private final int pageNumber;
        private final int cols;
        private final int rows;
        private final String name;
        private final int styleKey;

        public Category(int pageNumber, int cols, int rows, String name, int styleKey) {
            this.pageNumber = pageNumber;
            this.cols = cols;
            this.rows = rows;
            this.name = name;
            this.styleKey = styleKey;
        }

        public int getPageNumber() { return pageNumber; }
        public int getCols() { return cols; }
        public int getRows() { return rows; }
        public String getName() { return name; }
        public int getStyleKey() { return styleKey; }
    }

    public static class Page {
        private final int pageNumber;
        private final int cols;
        private final int rows;
        private final List<Button> buttons;

        public Page(int pageNumber, int cols, int rows, List<Button> buttons) {
            this.pageNumber = pageNumber;
            this.cols = cols;
            this.rows = rows;
            this.buttons = buttons;
        }

        public int getPageNumber() { return pageNumber; }
        public int getCols() { return cols; }
        public int getRows() { return rows; }
        public List<Button> getButtons() { return buttons; }
    }

    public static class Button {
        private final int col;
        private final int row;
        private final String label;
        private final int styleKey;
        private final String itemCode; // SettingData

        public Button(int col, int row, String label, int styleKey, String itemCode) {
            this.col = col;
            this.row = row;
            this.label = label;
            this.styleKey = styleKey;
            this.itemCode = itemCode;
        }

        public int getCol() { return col; }
        public int getRow() { return row; }
        public String getLabel() { return label; }
        public int getStyleKey() { return styleKey; }
        public String getItemCode() { return itemCode; }
    }
}
