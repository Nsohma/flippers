package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import com.example.demo.model.PosDraft;
import com.example.demo.service.exception.NotFoundException;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;

import java.io.ByteArrayInputStream;

final class DraftServiceSupport {
    private DraftServiceSupport() {
    }

    static PosDraft requireDraft(DraftRepository draftRepository, String draftId) {
        return draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));
    }

    static PosConfig.Page requirePage(PosDraft draft, int pageNumber) {
        PosConfig.Page page = draft.getConfig().getPage(pageNumber);
        if (page == null) {
            throw new NotFoundException("page not found: " + pageNumber);
        }
        return page;
    }

    static void saveUpdatedDraft(DraftRepository draftRepository, PosDraft draft, PosConfig updatedConfig) {
        saveUpdatedDraft(draftRepository, draft, updatedConfig, draft.getItemCatalogOrNull());
    }

    static void saveUpdatedDraft(
            DraftRepository draftRepository,
            PosDraft draft,
            PosConfig updatedConfig,
            ItemCatalog itemCatalog
    ) {
        saveUpdatedDraft(draftRepository, draft, updatedConfig, itemCatalog, "編集");
    }

    static void saveUpdatedDraft(
            DraftRepository draftRepository,
            PosDraft draft,
            PosConfig updatedConfig,
            ItemCatalog itemCatalog,
            String action
    ) {
        PosDraft baseDraft = draft.withItemCatalog(itemCatalog);
        PosDraft updatedDraft = baseDraft.applyNewConfig(updatedConfig, action);
        draftRepository.save(updatedDraft);
    }

    static ItemCatalog loadItemCatalog(PosDraft draft, PosConfigReader reader, DraftRepository draftRepository) {
        ItemCatalog cached = draft.getItemCatalogOrNull();
        if (cached != null) {
            return cached;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(draft.getOriginalExcelBytes())) {
            PosConfigSource source = reader.read(in);
            ItemCatalog loaded = source.getItemCatalog();
            PosDraft cachedDraft = draft.withItemCatalog(loaded);
            draftRepository.save(cachedDraft);
            return loaded;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to read item catalog", ex);
        }
    }

    static int resolveStyleKey(PosConfig config, PosConfig.Page page, int pageNumber) {
        if (!page.getButtons().isEmpty()) {
            return page.getButtons().get(0).getStyleKey();
        }
        for (PosConfig.Category category : config.getCategories()) {
            if (category.getPageNumber() == pageNumber) {
                return category.getStyleKey();
            }
        }
        return 1;
    }

    static String resolveCategoryName(PosConfig config, int pageNumber) {
        for (PosConfig.Category category : config.getCategories()) {
            if (category.getPageNumber() == pageNumber) {
                return displayText(category.getName(), "ページ" + pageNumber);
            }
        }
        return "ページ" + pageNumber;
    }

    static PosConfig.Button requireButton(PosConfig.Page page, String buttonId) {
        if (buttonId == null || buttonId.isBlank()) {
            throw new IllegalArgumentException("buttonId is required");
        }
        for (PosConfig.Button button : page.getButtons()) {
            if (buttonId.equals(button.getButtonId())) {
                return button;
            }
        }
        throw new IllegalArgumentException("button not found: " + buttonId);
    }

    static String resolveButtonName(PosConfig.Button button) {
        String fromLabel = displayTextOrNull(button.getLabel());
        if (fromLabel != null) {
            return fromLabel;
        }
        String fromItemCode = displayTextOrNull(button.getItemCode());
        if (fromItemCode != null) {
            return fromItemCode;
        }
        return displayText(button.getButtonId(), "商品不明");
    }

    static String resolveItemName(String itemName, String itemCode) {
        String fromName = displayTextOrNull(itemName);
        if (fromName != null) {
            return fromName;
        }
        return displayText(itemCode, "商品不明");
    }

    static String formatButtonAction(String baseAction, String categoryName, String itemName) {
        String normalizedAction = displayText(baseAction, "編集");
        String normalizedCategory = displayText(categoryName, "カテゴリ不明");
        String normalizedItem = displayText(itemName, "商品不明");
        return normalizedAction + " (" + normalizedCategory + "、" + normalizedItem + ")";
    }

    static String normalizeUnitPrice(String unitPrice) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice is required");
        }

        String normalized = unitPrice.trim().replace(",", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("unitPrice is required");
        }
        if (!normalized.matches("^\\d+(\\.\\d+)?$")) {
            throw new IllegalArgumentException("unitPrice must be numeric");
        }
        return normalized;
    }

    private static String displayText(String value, String fallback) {
        String normalized = displayTextOrNull(value);
        if (normalized != null) {
            return normalized;
        }
        return fallback;
    }

    private static String displayTextOrNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }
}
