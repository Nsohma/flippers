package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class AddCategoryService implements AddCategoryUseCase {

    private final DraftRepository draftRepository;

    public AddCategoryService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosConfig addCategory(String draftId, String name, int cols, int rows, int styleKey) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);

        int resolvedCols = cols;
        int resolvedRows = rows;
        int resolvedStyleKey = styleKey;
        PosConfig.Category defaultCategory = draft.getConfig().firstCategoryOrNull();
        if (resolvedCols <= 0 && defaultCategory != null) {
            resolvedCols = defaultCategory.getCols();
        }
        if (resolvedRows <= 0 && defaultCategory != null) {
            resolvedRows = defaultCategory.getRows();
        }
        if (resolvedStyleKey <= 0 && defaultCategory != null) {
            resolvedStyleKey = defaultCategory.getStyleKey();
        }
        if (resolvedCols <= 0) {
            resolvedCols = 5;
        }
        if (resolvedRows <= 0) {
            resolvedRows = 5;
        }
        if (resolvedStyleKey <= 0) {
            resolvedStyleKey = 1;
        }

        PosConfig updatedConfig = draft.getConfig().addCategory(name, resolvedCols, resolvedRows, resolvedStyleKey);
        DraftServiceSupport.saveUpdatedDraft(draftRepository, draft, updatedConfig, null, "カテゴリ追加");
        return updatedConfig;
    }
}
