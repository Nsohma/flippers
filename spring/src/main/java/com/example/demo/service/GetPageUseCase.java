package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface GetPageUseCase {
    PosConfig.Page getPage(String draftId, int pageNumber);
    PosConfig.Page swapButtons(String draftId, int pageNumber, int fromCol, int fromRow, int toCol, int toRow);
}
