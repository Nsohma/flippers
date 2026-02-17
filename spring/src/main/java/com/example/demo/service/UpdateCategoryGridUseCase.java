package com.example.demo.service;

import com.example.demo.model.PosDraft;

public interface UpdateCategoryGridUseCase {
    PosDraft updateCategoryGrid(String draftId, int pageNumber, int cols, int rows);
}
