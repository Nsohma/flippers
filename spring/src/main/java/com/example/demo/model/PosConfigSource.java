package com.example.demo.model;

import java.util.List;
import java.util.Objects;

public class PosConfigSource {
    private final List<PosConfig.Category> categories;
    private final List<PageButton> pageButtons;

    public PosConfigSource(List<PosConfig.Category> categories, List<PageButton> pageButtons) {
        this.categories = List.copyOf(Objects.requireNonNull(categories));
        this.pageButtons = List.copyOf(Objects.requireNonNull(pageButtons));
    }

    public List<PosConfig.Category> getCategories() {
        return categories;
    }

    public List<PageButton> getPageButtons() {
        return pageButtons;
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
