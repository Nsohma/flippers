package com.example.demo.controller.dto;

import com.example.demo.model.ItemCatalog;

import java.util.List;
import java.util.stream.Collectors;

public class ItemCatalogResponse {
    public List<CategoryDto> categories;

    public static ItemCatalogResponse from(ItemCatalog catalog) {
        ItemCatalogResponse response = new ItemCatalogResponse();
        response.categories = catalog.getCategories().stream().map(category -> {
            CategoryDto dto = new CategoryDto();
            dto.code = category.getCode();
            dto.description = category.getDescription();
            dto.items = category.getItems().stream().map(item -> {
                ItemDto itemDto = new ItemDto();
                itemDto.itemCode = item.getItemCode();
                itemDto.itemName = item.getItemName();
                itemDto.unitPrice = item.getUnitPrice();
                return itemDto;
            }).collect(Collectors.toList());
            return dto;
        }).collect(Collectors.toList());
        return response;
    }

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
