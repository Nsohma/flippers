package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface AddCategoryUseCase {
    PosConfig addCategory(String draftId, String name, int cols, int rows, int styleKey);
}
