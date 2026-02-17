package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface SwapButtonsUseCase {
    PosConfig.Page swapButtons(
            String draftId,
            int pageNumber,
            int fromCol,
            int fromRow,
            int toCol,
            int toRow
    );
}
