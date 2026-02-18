package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface SwapHandyCategoriesUseCase {
    ItemCatalog swap(String draftId, String fromCategoryCode, String toCategoryCode);
}
