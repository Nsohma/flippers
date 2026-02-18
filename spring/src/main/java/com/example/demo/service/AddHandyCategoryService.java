package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AddHandyCategoryService implements AddHandyCategoryUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public AddHandyCategoryService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog add(String draftId, String description) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        ItemCatalog handyCatalog = DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
        PosDraft baseDraft = draft.getHandyCatalogOrNull() == null ? draft.withHandyCatalog(handyCatalog) : draft;

        String normalizedCategoryCode = nextAutoCategoryCode(handyCatalog);
        String normalizedDescription = normalizeDescription(description, normalizedCategoryCode);
        ItemCatalog.Category category = new ItemCatalog.Category(
                normalizedCategoryCode,
                normalizedDescription,
                List.of()
        );
        int insertIndex = handyCatalog.getCategories().size();

        PosDraft.Change change = new PosDraft.AddHandyCategoryChange(insertIndex, category);
        String action = "ハンディカテゴリ追加 (" + normalizedDescription + ")";
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                baseDraft,
                change,
                action
        );

        ItemCatalog updatedCatalog = updatedDraft.getHandyCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("handy catalog not found after add category");
        }
        return updatedCatalog;
    }

    private static String nextAutoCategoryCode(ItemCatalog handyCatalog) {
        Set<String> usedCodes = new HashSet<>();
        for (ItemCatalog.Category category : handyCatalog.getCategories()) {
            if (category == null) {
                continue;
            }
            String code = category.getCode();
            if (code == null) {
                continue;
            }
            String normalized = code.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            usedCodes.add(normalized);
        }

        int next = 10;
        while (usedCodes.contains(String.valueOf(next))) {
            next += 1;
        }
        return String.valueOf(next);
    }

    private static String normalizeDescription(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return fallback;
    }
}
