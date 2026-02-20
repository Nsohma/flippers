package com.example.demo.service;

import com.example.demo.model.ItemMasterCatalog;

public interface UpdateItemMasterUseCase {
    ItemMasterCatalog updateItem(
            String draftId,
            String currentItemCode,
            String itemCode,
            String itemNamePrint,
            String unitPrice,
            String costPrice,
            String basePrice
    );
}
