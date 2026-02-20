package com.example.demo.model;

import java.util.List;
import java.util.Objects;

public class PosConfigSource {
    private final List<PosConfig.Category> categories;
    private final List<PageButton> pageButtons;
    private final ItemCatalog itemCatalog;
    private final ItemCatalog handyCatalog;
    private final ItemMasterCatalog itemMasterCatalog;

    public PosConfigSource(List<PosConfig.Category> categories, List<PageButton> pageButtons) {
        this(categories, pageButtons, ItemCatalog.empty(), ItemCatalog.empty(), ItemMasterCatalog.empty());
    }

    public PosConfigSource(
            List<PosConfig.Category> categories,
            List<PageButton> pageButtons,
            ItemCatalog itemCatalog
    ) {
        this(categories, pageButtons, itemCatalog, ItemCatalog.empty(), ItemMasterCatalog.empty());
    }

    public PosConfigSource(
            List<PosConfig.Category> categories,
            List<PageButton> pageButtons,
            ItemCatalog itemCatalog,
            ItemCatalog handyCatalog
    ) {
        this(categories, pageButtons, itemCatalog, handyCatalog, ItemMasterCatalog.empty());
    }

    public PosConfigSource(
            List<PosConfig.Category> categories,
            List<PageButton> pageButtons,
            ItemCatalog itemCatalog,
            ItemCatalog handyCatalog,
            ItemMasterCatalog itemMasterCatalog
    ) {
        this.categories = List.copyOf(Objects.requireNonNull(categories));
        this.pageButtons = List.copyOf(Objects.requireNonNull(pageButtons));
        this.itemCatalog = Objects.requireNonNull(itemCatalog);
        this.handyCatalog = Objects.requireNonNull(handyCatalog);
        this.itemMasterCatalog = Objects.requireNonNull(itemMasterCatalog);
    }

    public List<PosConfig.Category> getCategories() {
        return categories;
    }

    public List<PageButton> getPageButtons() {
        return pageButtons;
    }

    public ItemCatalog getItemCatalog() {
        return itemCatalog;
    }

    public ItemCatalog getHandyCatalog() {
        return handyCatalog;
    }

    public ItemMasterCatalog getItemMasterCatalog() {
        return itemMasterCatalog;
    }

    public static class PageButton {
        private final int pageNumber;
        private final PosConfig.Button button;

        public PageButton(int pageNumber, PosConfig.Button button) {
            this.pageNumber = pageNumber;
            this.button = Objects.requireNonNull(button);
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public PosConfig.Button getButton() {
            return button;
        }
    }
}
