package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface DeleteHandyCategoryUseCase {
    ItemCatalog delete(String draftId, String categoryCode);
}
