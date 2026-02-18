package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeleteHandyCategoryService implements DeleteHandyCategoryUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public DeleteHandyCategoryService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog delete(String draftId, String categoryCode) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        ItemCatalog handyCatalog = DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
        PosDraft baseDraft = draft.getHandyCatalogOrNull() == null ? draft.withHandyCatalog(handyCatalog) : draft;

        String normalizedCategoryCode = normalizeRequired(categoryCode, "categoryCode");
        List<ItemCatalog.Category> categories = handyCatalog.getCategories();

        int categoryIndex = -1;
        ItemCatalog.Category targetCategory = null;
        for (int i = 0; i < categories.size(); i++) {
            ItemCatalog.Category category = categories.get(i);
            if (!normalizedCategoryCode.equals(category.getCode())) {
                continue;
            }
            categoryIndex = i;
            targetCategory = category;
            break;
        }
        if (categoryIndex < 0 || targetCategory == null) {
            throw new IllegalArgumentException("handy category not found: " + normalizedCategoryCode);
        }

        PosDraft.Change change = new PosDraft.DeleteHandyCategoryChange(categoryIndex, targetCategory);
        String categoryName = targetCategory.getDescription() == null || targetCategory.getDescription().isBlank()
                ? normalizedCategoryCode
                : targetCategory.getDescription();
        String action = "ハンディカテゴリ削除 (" + categoryName + ")";

        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                baseDraft,
                change,
                action
        );

        ItemCatalog updatedCatalog = updatedDraft.getHandyCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("handy catalog not found after delete category");
        }
        return updatedCatalog;
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }
}
