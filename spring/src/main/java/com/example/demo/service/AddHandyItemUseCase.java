package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface AddHandyItemUseCase {
    ItemCatalog add(String draftId, String handyCategoryCode, String sourceCategoryCode, String itemCode);
}
