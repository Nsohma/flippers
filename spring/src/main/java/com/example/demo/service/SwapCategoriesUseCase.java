package com.example.demo.service;

import com.example.demo.model.PosDraft;

public interface SwapCategoriesUseCase {
    PosDraft swapCategories(String draftId, int fromPageNumber, int toPageNumber);
}
