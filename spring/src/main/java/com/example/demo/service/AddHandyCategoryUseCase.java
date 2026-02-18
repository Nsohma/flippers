package com.example.demo.service;

import com.example.demo.model.ItemCatalog;

public interface AddHandyCategoryUseCase {
    ItemCatalog add(String draftId, String description);
}
