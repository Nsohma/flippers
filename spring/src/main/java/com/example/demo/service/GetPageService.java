package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import com.example.demo.model.PosDraft;
import com.example.demo.service.exception.NotFoundException;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class GetPageService implements GetPageUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public GetPageService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public PosConfig.Page getPage(String draftId, int pageNumber) {
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));
        PosConfig.Page page = draft.getConfig().getPage(pageNumber);
        if (page == null) throw new NotFoundException("page not found: " + pageNumber);
        return page;
    }

    @Override
    public PosConfig.Page swapButtons(
            String draftId,
            int pageNumber,
            int fromCol,
            int fromRow,
            int toCol,
            int toRow
    ) {
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));

        PosConfig.Page currentPage = draft.getConfig().getPage(pageNumber);
        if (currentPage == null) throw new NotFoundException("page not found: " + pageNumber);

        PosConfig updatedConfig = draft.getConfig().swapButtons(pageNumber, fromCol, fromRow, toCol, toRow);
        PosDraft updatedDraft = new PosDraft(draftId, updatedConfig, draft.getOriginalExcelBytes());
        draftRepository.save(updatedDraft);
        return updatedConfig.getPage(pageNumber);
    }

    @Override
    public ItemCatalog getItemCatalog(String draftId) {
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));
        return loadItemCatalog(draft);
    }

    @Override
    public PosConfig.Page addButton(
            String draftId,
            int pageNumber,
            int col,
            int row,
            String categoryCode,
            String itemCode
    ) {
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));

        PosConfig.Page currentPage = draft.getConfig().getPage(pageNumber);
        if (currentPage == null) throw new NotFoundException("page not found: " + pageNumber);

        ItemCatalog catalog = loadItemCatalog(draft);
        ItemCatalog.Category category = catalog.findCategory(categoryCode);
        if (category == null) {
            throw new IllegalArgumentException("category not found: " + categoryCode);
        }
        ItemCatalog.Item item = category.findItem(itemCode);
        if (item == null) {
            throw new IllegalArgumentException("item not found in category: " + itemCode);
        }

        int styleKey = resolveStyleKey(draft.getConfig(), currentPage, pageNumber);
        String label = item.getItemName() == null || item.getItemName().isBlank()
                ? item.getItemCode()
                : item.getItemName();
        String newButtonId = "PresetMenuButtonMaster#NEW-" + UUID.randomUUID();

        PosConfig updatedConfig = draft.getConfig().addButton(
                pageNumber,
                col,
                row,
                label,
                styleKey,
                item.getItemCode(),
                item.getUnitPrice(),
                newButtonId
        );
        PosDraft updatedDraft = new PosDraft(draftId, updatedConfig, draft.getOriginalExcelBytes());
        draftRepository.save(updatedDraft);
        return updatedConfig.getPage(pageNumber);
    }

    @Override
    public PosConfig.Page deleteButton(String draftId, int pageNumber, String buttonId) {
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));

        PosConfig.Page currentPage = draft.getConfig().getPage(pageNumber);
        if (currentPage == null) throw new NotFoundException("page not found: " + pageNumber);

        PosConfig updatedConfig = draft.getConfig().deleteButton(pageNumber, buttonId);
        PosDraft updatedDraft = new PosDraft(draftId, updatedConfig, draft.getOriginalExcelBytes());
        draftRepository.save(updatedDraft);
        return updatedConfig.getPage(pageNumber);
    }

    private ItemCatalog loadItemCatalog(PosDraft draft) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(draft.getOriginalExcelBytes())) {
            PosConfigSource source = reader.read(in);
            return source.getItemCatalog();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to read item catalog", ex);
        }
    }

    private static int resolveStyleKey(PosConfig config, PosConfig.Page page, int pageNumber) {
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
}
