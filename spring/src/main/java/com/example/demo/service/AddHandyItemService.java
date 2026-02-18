package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddHandyItemService implements AddHandyItemUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public AddHandyItemService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog add(String draftId, String handyCategoryCode, String sourceCategoryCode, String itemCode) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        ItemCatalog handyCatalog = DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
        ItemCatalog itemCatalog = DraftServiceSupport.loadItemCatalog(draft, reader, draftRepository);

        String normalizedHandyCategoryCode = normalizeRequired(handyCategoryCode, "handyCategoryCode");
        String normalizedSourceCategoryCode = normalizeRequired(sourceCategoryCode, "sourceCategoryCode");
        String normalizedItemCode = normalizeRequired(itemCode, "itemCode");

        ItemCatalog.Category handyCategory = findCategory(handyCatalog, normalizedHandyCategoryCode, "handy");
        ItemCatalog.Category sourceCategory = itemCatalog.findCategory(normalizedSourceCategoryCode);
        if (sourceCategory == null) {
            throw new IllegalArgumentException("source category not found: " + normalizedSourceCategoryCode);
        }

        ItemCatalog.Item sourceItem = sourceCategory.findItem(normalizedItemCode);
        if (sourceItem == null) {
            throw new IllegalArgumentException("item not found in source category: " + normalizedItemCode);
        }

        int insertIndex = resolveInsertIndex(handyCategory, sourceCategory, normalizedItemCode);
        String resolvedItemName = DraftServiceSupport.resolveItemName(sourceItem.getItemName(), sourceItem.getItemCode());
        ItemCatalog.Item addedItem = new ItemCatalog.Item(
                sourceItem.getItemCode(),
                resolvedItemName,
                sourceItem.getUnitPrice()
        );

        PosDraft.Change change = new PosDraft.AddHandyItemChange(
                normalizedHandyCategoryCode,
                insertIndex,
                addedItem
        );

        String categoryName = handyCategory.getDescription() == null || handyCategory.getDescription().isBlank()
                ? normalizedHandyCategoryCode
                : handyCategory.getDescription();
        String action = DraftServiceSupport.formatButtonAction("ハンディ商品追加", categoryName, resolvedItemName);

        PosDraft baseDraft = draft;
        if (baseDraft.getItemCatalogOrNull() == null) {
            baseDraft = baseDraft.withItemCatalog(itemCatalog);
        }
        if (baseDraft.getHandyCatalogOrNull() == null) {
            baseDraft = baseDraft.withHandyCatalog(handyCatalog);
        }

        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                baseDraft,
                change,
                action
        );
        ItemCatalog updatedCatalog = updatedDraft.getHandyCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("handy catalog not found after add");
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

    private static ItemCatalog.Category findCategory(ItemCatalog catalog, String categoryCode, String kind) {
        ItemCatalog.Category category = catalog.findCategory(categoryCode);
        if (category == null) {
            throw new IllegalArgumentException(kind + " category not found: " + categoryCode);
        }
        return category;
    }

    private static int resolveInsertIndex(
            ItemCatalog.Category handyCategory,
            ItemCatalog.Category sourceCategory,
            String itemCode
    ) {
        Map<String, Integer> sourceOrderByItemCode = new HashMap<>();
        int order = 0;
        for (ItemCatalog.Item sourceItem : sourceCategory.getItems()) {
            String sourceItemCode = sourceItem.getItemCode();
            if (!sourceOrderByItemCode.containsKey(sourceItemCode)) {
                sourceOrderByItemCode.put(sourceItemCode, order);
                order += 1;
            }
        }

        Integer targetOrder = sourceOrderByItemCode.get(itemCode);
        if (targetOrder == null) {
            return handyCategory.getItems().size();
        }

        List<ItemCatalog.Item> existingItems = handyCategory.getItems();
        for (int i = 0; i < existingItems.size(); i++) {
            Integer existingOrder = sourceOrderByItemCode.get(existingItems.get(i).getItemCode());
            if (existingOrder == null) {
                continue;
            }
            if (targetOrder < existingOrder) {
                return i;
            }
        }
        return existingItems.size();
    }
}
