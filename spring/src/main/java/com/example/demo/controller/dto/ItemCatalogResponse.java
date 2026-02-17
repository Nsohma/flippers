package com.example.demo.controller.dto;

import java.util.List;

public class ItemCatalogResponse {
    public List<CategoryDto> categories;

    public static class CategoryDto {
        public String code;
        public String description;
        public List<ItemDto> items;
    }

    public static class ItemDto {
        public String itemCode;
        public String itemName;
        public String unitPrice;
    }
}
