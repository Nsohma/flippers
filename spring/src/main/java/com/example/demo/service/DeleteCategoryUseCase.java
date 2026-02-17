package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface DeleteCategoryUseCase {
    PosConfig deleteCategory(String draftId, int pageNumber);
}
