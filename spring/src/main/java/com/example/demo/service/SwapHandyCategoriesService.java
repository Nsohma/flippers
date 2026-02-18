package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

@Service
public class SwapHandyCategoriesService implements SwapHandyCategoriesUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public SwapHandyCategoriesService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog swap(String draftId, String fromCategoryCode, String toCategoryCode) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        ItemCatalog handyCatalog = DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
        PosDraft baseDraft = draft.getHandyCatalogOrNull() == null ? draft.withHandyCatalog(handyCatalog) : draft;

        String normalizedFromCategoryCode = normalizeRequired(fromCategoryCode, "fromCategoryCode");
        String normalizedToCategoryCode = normalizeRequired(toCategoryCode, "toCategoryCode");
        if (normalizedFromCategoryCode.equals(normalizedToCategoryCode)) {
            return handyCatalog;
        }

        ItemCatalog.Category fromCategory = findCategory(handyCatalog, normalizedFromCategoryCode);
        ItemCatalog.Category toCategory = findCategory(handyCatalog, normalizedToCategoryCode);

        PosDraft.Change change = new PosDraft.SwapHandyCategoriesChange(
                normalizedFromCategoryCode,
                normalizedToCategoryCode
        );
        String fromName = resolveCategoryName(fromCategory, normalizedFromCategoryCode);
        String toName = resolveCategoryName(toCategory, normalizedToCategoryCode);
        String action = "ハンディカテゴリ入れ替え (" + fromName + " <-> " + toName + ")";
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                baseDraft,
                change,
                action
        );

        ItemCatalog updatedCatalog = updatedDraft.getHandyCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("handy catalog not found after swap categories");
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

    private static ItemCatalog.Category findCategory(ItemCatalog handyCatalog, String categoryCode) {
        ItemCatalog.Category category = handyCatalog.findCategory(categoryCode);
        if (category == null) {
            throw new IllegalArgumentException("handy category not found: " + categoryCode);
        }
        return category;
    }

    private static String resolveCategoryName(ItemCatalog.Category category, String fallback) {
        String description = category.getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }
        return fallback;
    }
}
