package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface ReorderHandyItemsUseCase {
    ItemCatalog reorder(String draftId, String categoryCode, int fromIndex, int toIndex);
}
