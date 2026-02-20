package com.example.demo.service;

import com.example.demo.model.ItemMasterCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

@Service
public class UpdateItemMasterService implements UpdateItemMasterUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public UpdateItemMasterService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemMasterCatalog updateItem(
            String draftId,
            String currentItemCode,
            String itemCode,
            String itemNamePrint,
            String unitPrice,
            String costPrice,
            String basePrice
    ) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        DraftServiceSupport.loadItemMasterCatalog(draft, reader, draftRepository);
        PosDraft latestDraft = draftRepository.findById(draft.getDraftId()).orElse(draft);

        ItemMasterCatalog catalog = latestDraft.getItemMasterCatalogOrNull();
        if (catalog == null) {
            throw new IllegalStateException("item master catalog is not loaded");
        }

        String currentCode = normalizeRequired(currentItemCode, "currentItemCode");
        ItemMasterCatalog.Item beforeItem = findItemByCode(catalog, currentCode);

        String nextCode = normalizeRequired(itemCode, "itemCode");
        if (!currentCode.equals(nextCode) && findItemOrNull(catalog, nextCode) != null) {
            throw new IllegalArgumentException("itemCode already exists: " + nextCode);
        }

        String nextNamePrint = normalizeRequired(itemNamePrint, "itemNamePrint");
        String nextUnitPrice = normalizeRequiredNumeric(unitPrice, "unitPrice");
        String nextCostPrice = normalizeOptionalNumeric(costPrice, "costPrice");
        String nextBasePrice = normalizeOptionalNumeric(basePrice, "basePrice");

        ItemMasterCatalog.Item afterItem = new ItemMasterCatalog.Item(
                nextCode,
                nextNamePrint,
                nextUnitPrice,
                nextCostPrice,
                nextBasePrice
        );
        PosDraft.Change change = new PosDraft.UpdateItemMasterItemChange(beforeItem, afterItem);
        String action = "商品マスタ更新 (" + beforeItem.getItemCode() + "、" + afterItem.getItemNamePrint() + ")";
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                latestDraft,
                change,
                action
        );

        ItemMasterCatalog updatedCatalog = updatedDraft.getItemMasterCatalogOrNull();
        if (updatedCatalog == null) {
            throw new IllegalStateException("item master catalog update failed");
        }
        return updatedCatalog;
    }

    private static ItemMasterCatalog.Item findItemByCode(ItemMasterCatalog catalog, String itemCode) {
        ItemMasterCatalog.Item item = findItemOrNull(catalog, itemCode);
        if (item == null) {
            throw new IllegalArgumentException("item not found in item master: " + itemCode);
        }
        return item;
    }

    private static ItemMasterCatalog.Item findItemOrNull(ItemMasterCatalog catalog, String itemCode) {
        for (ItemMasterCatalog.Item item : catalog.getItems()) {
            if (itemCode.equals(item.getItemCode())) {
                return item;
            }
        }
        return null;
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private static String normalizeRequiredNumeric(String value, String fieldName) {
        String normalized = normalizeOptionalNumeric(value, fieldName);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private static String normalizeOptionalNumeric(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim().replace(",", "");
        if (normalized.isEmpty()) {
            return "";
        }
        if (!normalized.matches("^\\d+(\\.\\d+)?$")) {
            throw new IllegalArgumentException(fieldName + " must be numeric");
        }
        return normalized;
    }
}
