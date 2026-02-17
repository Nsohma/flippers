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

        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("category name is required");
        }
        int nextPageNumber = draft.getConfig().getCategories().stream()
                .mapToInt(PosConfig.Category::getPageNumber)
                .max()
                .orElse(0) + 1;
        PosConfig.Category newCategory = new PosConfig.Category(
                nextPageNumber,
                resolvedCols,
                resolvedRows,
                normalizedName,
                resolvedStyleKey
        );
        PosDraft.Change change = new PosDraft.AddCategoryChange(newCategory);
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                draft,
                change,
                "カテゴリ追加"
        );
        return updatedDraft.getConfig();
    }
}
