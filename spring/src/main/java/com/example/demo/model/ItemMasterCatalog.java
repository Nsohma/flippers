package com.example.demo.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ItemMasterCatalog implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Item> items;

    public ItemMasterCatalog(List<Item> items) {
        this.items = List.copyOf(Objects.requireNonNull(items));
    }

    public List<Item> getItems() {
        return items;
    }

    public static ItemMasterCatalog empty() {
        return new ItemMasterCatalog(List.of());
    }

    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String itemCode;
        private final String itemNamePrint;
        private final String unitPrice;
        private final String costPrice;
        private final String basePrice;

        public Item(
                String itemCode,
                String itemNamePrint,
                String unitPrice,
                String costPrice,
                String basePrice
        ) {
            this.itemCode = Objects.requireNonNull(itemCode);
            this.itemNamePrint = Objects.requireNonNull(itemNamePrint);
            this.unitPrice = Objects.requireNonNull(unitPrice);
            this.costPrice = Objects.requireNonNull(costPrice);
            this.basePrice = Objects.requireNonNull(basePrice);
        }

        public String getItemCode() {
            return itemCode;
        }

        public String getItemNamePrint() {
            return itemNamePrint;
        }

        public String getUnitPrice() {
            return unitPrice;
        }

        public String getCostPrice() {
            return costPrice;
        }

        public String getBasePrice() {
            return basePrice;
        }
    }
}
