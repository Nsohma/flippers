package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface ReorderHandyCategoriesUseCase {
    ItemCatalog reorder(String draftId, int fromIndex, int toIndex);
}

