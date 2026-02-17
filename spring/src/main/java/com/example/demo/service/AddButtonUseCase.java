package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface AddButtonUseCase {
    PosConfig.Page addButton(
            String draftId,
            int pageNumber,
            int col,
            int row,
            String categoryCode,
            String itemCode
    );
}
