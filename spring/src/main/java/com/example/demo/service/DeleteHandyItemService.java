package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

@Service
public class DeleteHandyItemService implements DeleteHandyItemUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public DeleteHandyItemService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog delete(String draftId, String categoryCode, int itemIndex) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        ItemCatalog handyCatalog = DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
        PosDraft baseDraft = draft.getHandyCatalogOrNull() == null ? draft.withHandyCatalog(handyCatalog) : draft;

        String normalizedCategoryCode = categoryCode == null ? "" : categoryCode.trim();
        if (normalizedCategoryCode.isEmpty()) {
            throw new IllegalArgumentException("categoryCode is required");
        }

        ItemCatalog.Category category = findCategory(handyCatalog, normalizedCategoryCode);
        if (itemIndex < 0 || itemIndex >= category.getItems().size()) {
            throw new IllegalArgumentException("itemIndex out of range: " + itemIndex);
        }
        ItemCatalog.Item deletedItem = category.getItems().get(itemIndex);

        PosDraft.Change change = new PosDraft.DeleteHandyItemChange(
                normalizedCategoryCode,
                itemIndex,
                deletedItem
        );
        String categoryName = category.getDescription() == null || category.getDescription().isBlank()
                ? normalizedCategoryCode
                : category.getDescription();
        String itemName = DraftServiceSupport.resolveItemName(deletedItem.getItemName(), deletedItem.getItemCode());
        String action = DraftServiceSupport.formatButtonAction("ハンディ商品削除", categoryName, itemName);
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                baseDraft,
                change,
                action
        );
        ItemCatalog updatedCatalog = updatedDraft.getHandyCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("handy catalog not found after delete");
        }
        return updatedCatalog;
    }

    private static ItemCatalog.Category findCategory(ItemCatalog handyCatalog, String categoryCode) {
        for (ItemCatalog.Category category : handyCatalog.getCategories()) {
            if (categoryCode.equals(category.getCode())) {
                return category;
            }
        }
        throw new IllegalArgumentException("handy category not found: " + categoryCode);
    }
}
