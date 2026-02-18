package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

@Service
public class ReorderHandyCategoriesService implements ReorderHandyCategoriesUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public ReorderHandyCategoriesService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog reorder(String draftId, int fromIndex, int toIndex) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        ItemCatalog handyCatalog = DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
        PosDraft baseDraft = draft.getHandyCatalogOrNull() == null ? draft.withHandyCatalog(handyCatalog) : draft;

        int categoryCount = handyCatalog.getCategories().size();
        if (fromIndex < 0 || fromIndex >= categoryCount) {
            throw new IllegalArgumentException("fromIndex out of range: " + fromIndex);
        }
        if (toIndex < 0 || toIndex >= categoryCount) {
            throw new IllegalArgumentException("toIndex out of range: " + toIndex);
        }
        if (fromIndex == toIndex) {
            return handyCatalog;
        }

        ItemCatalog.Category movedCategory = handyCatalog.getCategories().get(fromIndex);
        String categoryName = resolveCategoryName(movedCategory);

        PosDraft.Change change = new PosDraft.ReorderHandyCategoriesChange(fromIndex, toIndex);
        String action = "ハンディカテゴリ移動 (" + categoryName + ")";
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                baseDraft,
                change,
                action
        );

        ItemCatalog updatedCatalog = updatedDraft.getHandyCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("handy catalog not found after reorder categories");
        }
        return updatedCatalog;
    }

    private static String resolveCategoryName(ItemCatalog.Category category) {
        String description = category.getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }
        String code = category.getCode();
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "カテゴリ不明";
    }
}

