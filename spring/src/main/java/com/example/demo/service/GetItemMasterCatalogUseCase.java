package com.example.demo.service;

import com.example.demo.model.ItemMasterCatalog;

public interface GetItemMasterCatalogUseCase {
    ItemMasterCatalog getItemMasterCatalog(String draftId);
}
