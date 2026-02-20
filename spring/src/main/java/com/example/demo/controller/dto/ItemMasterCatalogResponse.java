package com.example.demo.controller.dto;

import com.example.demo.model.ItemMasterCatalog;

import java.util.List;
import java.util.stream.Collectors;

public class ItemMasterCatalogResponse {
    public List<ItemDto> items;

    public static ItemMasterCatalogResponse from(ItemMasterCatalog catalog) {
        ItemMasterCatalogResponse response = new ItemMasterCatalogResponse();
        response.items = catalog.getItems().stream().map(item -> {
            ItemDto dto = new ItemDto();
            dto.itemCode = item.getItemCode();
            dto.itemNamePrint = item.getItemNamePrint();
            dto.unitPrice = item.getUnitPrice();
            dto.costPrice = item.getCostPrice();
            dto.basePrice = item.getBasePrice();
            return dto;
        }).collect(Collectors.toList());
        return response;
    }

    public static class ItemDto {
        public String itemCode;
        public String itemNamePrint;
        public String unitPrice;
        public String costPrice;
        public String basePrice;
    }
}
