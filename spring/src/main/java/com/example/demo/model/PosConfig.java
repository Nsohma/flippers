package com.example.demo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PosConfig implements Serializable {
    private static final long serialVersionUID = 1L;

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

    public PosConfig swapButtons(int pageNumber, int fromCol, int fromRow, int toCol, int toRow) {
        Page page = pagesByPageNumber.get(pageNumber);
        if (page == null) {
            throw new IllegalArgumentException("page not found: " + pageNumber);
        }

        Page swappedPage = page.swapButtons(fromCol, fromRow, toCol, toRow);
        Map<Integer, Page> updatedPages = new LinkedHashMap<>(pagesByPageNumber);
        updatedPages.put(pageNumber, swappedPage);
        return new PosConfig(categories, Collections.unmodifiableMap(updatedPages));
    }

    public PosConfig addButton(
            int pageNumber,
            int col,
            int row,
            String label,
            int styleKey,
            String itemCode,
            String unitPrice,
            String buttonId
    ) {
        Page page = pagesByPageNumber.get(pageNumber);
        if (page == null) {
            throw new IllegalArgumentException("page not found: " + pageNumber);
        }
        if (buttonId == null || buttonId.isBlank()) {
            throw new IllegalArgumentException("buttonId is required");
        }
        if (hasButtonId(buttonId)) {
            throw new IllegalArgumentException("duplicate buttonId: " + buttonId);
        }

        Button newButton = new Button(col, row, label, styleKey, itemCode, unitPrice, buttonId);
        Page updatedPage = page.addButton(newButton);
        Map<Integer, Page> updatedPages = new LinkedHashMap<>(pagesByPageNumber);
        updatedPages.put(pageNumber, updatedPage);
        return new PosConfig(categories, Collections.unmodifiableMap(updatedPages));
    }

    public PosConfig deleteButton(int pageNumber, String buttonId) {
        Page page = pagesByPageNumber.get(pageNumber);
        if (page == null) {
            throw new IllegalArgumentException("page not found: " + pageNumber);
        }
        if (buttonId == null || buttonId.isBlank()) {
            throw new IllegalArgumentException("buttonId is required");
        }

        Page updatedPage = page.deleteButton(buttonId);
        Map<Integer, Page> updatedPages = new LinkedHashMap<>(pagesByPageNumber);
        updatedPages.put(pageNumber, updatedPage);
        return new PosConfig(categories, Collections.unmodifiableMap(updatedPages));
    }

    private boolean hasButtonId(String buttonId) {
        for (Page page : pagesByPageNumber.values()) {
            for (Button button : page.getButtons()) {
                if (buttonId.equals(button.getButtonId())) return true;
            }
        }
        return false;
    }

    public static PosConfig fromSource(PosConfigSource source) {
        List<Category> sortedCategories = new ArrayList<>(source.getCategories());
        sortedCategories.sort(Comparator.comparingInt(Category::getPageNumber));

        Map<Integer, List<Button>> buttonsByPage = new HashMap<>();
        for (PosConfigSource.PageButton pageButton : source.getPageButtons()) {
            buttonsByPage
                    .computeIfAbsent(pageButton.getPageNumber(), k -> new ArrayList<>())
                    .add(pageButton.getButton());
        }

        Map<Integer, Page> pages = new LinkedHashMap<>();
        for (Category category : sortedCategories) {
            pages.putIfAbsent(
                    category.getPageNumber(),
                    new Page(
                            category.getPageNumber(),
                            category.getCols(),
                            category.getRows(),
                            List.copyOf(buttonsByPage.getOrDefault(category.getPageNumber(), List.of()))
                    )
            );
        }

        return new PosConfig(
                List.copyOf(sortedCategories),
                Collections.unmodifiableMap(new LinkedHashMap<>(pages))
        );
    }

    // ---- Value-like domain classes ----
    public static class Category implements Serializable {
        private static final long serialVersionUID = 1L;

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

    public static class Page implements Serializable {
        private static final long serialVersionUID = 1L;

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

        public Page swapButtons(int fromCol, int fromRow, int toCol, int toRow) {
            if (fromCol == toCol && fromRow == toRow) {
                return this;
            }
            if (toCol < 1 || toCol > cols) {
                throw new IllegalArgumentException("target col out of range: " + toCol);
            }
            if (toRow < 1 || toRow > rows) {
                throw new IllegalArgumentException("target row out of range: " + toRow);
            }

            int fromIndex = -1;
            int toIndex = -1;
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                if (button.getCol() == fromCol && button.getRow() == fromRow) {
                    fromIndex = i;
                } else if (button.getCol() == toCol && button.getRow() == toRow) {
                    toIndex = i;
                }
            }

            if (fromIndex < 0) {
                throw new IllegalArgumentException("source button not found: (" + fromCol + "," + fromRow + ")");
            }

            List<Button> updatedButtons = new ArrayList<>(buttons);
            Button fromButton = updatedButtons.get(fromIndex);
            if (toIndex < 0) {
                updatedButtons.set(fromIndex, fromButton.withPosition(toCol, toRow));
                return new Page(pageNumber, cols, rows, List.copyOf(updatedButtons));
            }

            Button toButton = updatedButtons.get(toIndex);
            updatedButtons.set(fromIndex, toButton.withPosition(fromCol, fromRow));
            updatedButtons.set(toIndex, fromButton.withPosition(toCol, toRow));
            return new Page(pageNumber, cols, rows, List.copyOf(updatedButtons));
        }

        public Page addButton(Button button) {
            int col = button.getCol();
            int row = button.getRow();
            if (col < 1 || col > cols) {
                throw new IllegalArgumentException("col out of range: " + col);
            }
            if (row < 1 || row > rows) {
                throw new IllegalArgumentException("row out of range: " + row);
            }
            for (Button b : buttons) {
                if (b.getCol() == col && b.getRow() == row) {
                    throw new IllegalArgumentException("cell is already occupied: (" + col + "," + row + ")");
                }
            }

            List<Button> updated = new ArrayList<>(buttons);
            updated.add(button);
            return new Page(pageNumber, cols, rows, List.copyOf(updated));
        }

        public Page deleteButton(String buttonId) {
            int deleteIndex = -1;
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                if (buttonId.equals(button.getButtonId())) {
                    deleteIndex = i;
                    break;
                }
            }
            if (deleteIndex < 0) {
                throw new IllegalArgumentException("button not found: " + buttonId);
            }

            List<Button> updated = new ArrayList<>(buttons);
            updated.remove(deleteIndex);
            return new Page(pageNumber, cols, rows, List.copyOf(updated));
        }
    }

    public static class Button implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int col;
        private final int row;
        private final String label;
        private final int styleKey;
        private final String itemCode; // SettingData
        private final String unitPrice;
        private final String buttonId;

        public Button(int col, int row, String label, int styleKey, String itemCode, String unitPrice, String buttonId) {
            this.col = col;
            this.row = row;
            this.label = label;
            this.styleKey = styleKey;
            this.itemCode = itemCode;
            this.unitPrice = unitPrice;
            this.buttonId = buttonId;
        }

        public int getCol() { return col; }
        public int getRow() { return row; }
        public String getLabel() { return label; }
        public int getStyleKey() { return styleKey; }
        public String getItemCode() { return itemCode; }
        public String getUnitPrice() { return unitPrice; }
        public String getButtonId() { return buttonId; }

        public Button withPosition(int newCol, int newRow) {
            return new Button(newCol, newRow, label, styleKey, itemCode, unitPrice, buttonId);
        }
    }
}
