package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;

public interface GetPageUseCase {
    PosConfig.Page getPage(String draftId, int pageNumber);
    PosConfig.Page swapButtons(String draftId, int pageNumber, int fromCol, int fromRow, int toCol, int toRow);
    ItemCatalog getItemCatalog(String draftId);
    PosConfig.Page addButton(
            String draftId,
            int pageNumber,
            int col,
            int row,
            String categoryCode,
            String itemCode
    );
    PosConfig.Page deleteButton(String draftId, int pageNumber, String buttonId);
    PosConfig.Page updateUnitPrice(String draftId, int pageNumber, String buttonId, String unitPrice);
    PosConfig addCategory(String draftId, String name, int cols, int rows, int styleKey);
    PosConfig deleteCategory(String draftId, int pageNumber);
}
