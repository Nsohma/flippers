package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface DeleteHandyItemUseCase {
    ItemCatalog delete(String draftId, String categoryCode, int itemIndex);
}
