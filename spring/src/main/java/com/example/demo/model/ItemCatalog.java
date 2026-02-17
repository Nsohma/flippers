package com.example.demo.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ItemCatalog implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Category> categories;

    public ItemCatalog(List<Category> categories) {
        this.categories = List.copyOf(Objects.requireNonNull(categories));
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Category findCategory(String categoryCode) {
        if (categoryCode == null) return null;
        for (Category category : categories) {
            if (categoryCode.equals(category.getCode())) {
                return category;
            }
        }
        return null;
    }

    public static ItemCatalog empty() {
        return new ItemCatalog(List.of());
    }

    public static class Category implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String code;
        private final String description;
        private final List<Item> items;

        public Category(String code, String description, List<Item> items) {
            this.code = Objects.requireNonNull(code);
            this.description = Objects.requireNonNull(description);
            this.items = List.copyOf(Objects.requireNonNull(items));
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public List<Item> getItems() {
            return items;
        }

        public Item findItem(String itemCode) {
            if (itemCode == null) return null;
            for (Item item : items) {
                if (itemCode.equals(item.getItemCode())) {
                    return item;
                }
            }
            return null;
        }
    }

    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String itemCode;
        private final String itemName;
        private final String unitPrice;

        public Item(String itemCode, String itemName, String unitPrice) {
            this.itemCode = Objects.requireNonNull(itemCode);
            this.itemName = Objects.requireNonNull(itemName);
            this.unitPrice = Objects.requireNonNull(unitPrice);
        }

        public String getItemCode() {
            return itemCode;
        }

        public String getItemName() {
            return itemName;
        }

        public String getUnitPrice() {
            return unitPrice;
        }
    }
}
