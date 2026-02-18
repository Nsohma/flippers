package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

@Service
public class ReorderHandyItemsService implements ReorderHandyItemsUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public ReorderHandyItemsService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog reorder(String draftId, String categoryCode, int fromIndex, int toIndex) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        ItemCatalog handyCatalog = DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
        PosDraft baseDraft = draft.getHandyCatalogOrNull() == null ? draft.withHandyCatalog(handyCatalog) : draft;

        String normalizedCategoryCode = categoryCode == null ? "" : categoryCode.trim();
        if (normalizedCategoryCode.isEmpty()) {
            throw new IllegalArgumentException("categoryCode is required");
        }
        if (fromIndex == toIndex) {
            return handyCatalog;
        }

        String categoryName = resolveHandyCategoryName(handyCatalog, normalizedCategoryCode);
        PosDraft.Change change = new PosDraft.ReorderHandyItemsChange(normalizedCategoryCode, fromIndex, toIndex);
        String action = "ハンディ商品並び替え (" + categoryName + ")";
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                baseDraft,
                change,
                action
        );
        ItemCatalog updatedCatalog = updatedDraft.getHandyCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("handy catalog not found after reorder");
        }
        return updatedCatalog;
    }

    private static String resolveHandyCategoryName(ItemCatalog handyCatalog, String categoryCode) {
        for (ItemCatalog.Category category : handyCatalog.getCategories()) {
            if (!categoryCode.equals(category.getCode())) {
                continue;
            }
            String description = category.getDescription();
            if (description != null && !description.isBlank()) {
                return description;
            }
            return categoryCode;
        }
        throw new IllegalArgumentException("handy category not found: " + categoryCode);
    }
}
