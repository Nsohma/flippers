package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface GetHandyCatalogUseCase {
    ItemCatalog getHandyCatalog(String draftId);
}
