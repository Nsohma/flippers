package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface GetItemCatalogUseCase {
    ItemCatalog getItemCatalog(String draftId);
}
