package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AddButtonService implements AddButtonUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public AddButtonService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
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
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosConfig.Page currentPage = DraftServiceSupport.requirePage(draft, pageNumber);

        ItemCatalog catalog = DraftServiceSupport.loadItemCatalog(draft, reader, draftRepository);
        ItemCatalog.Category category = catalog.findCategory(categoryCode);
        if (category == null) {
            throw new IllegalArgumentException("category not found: " + categoryCode);
        }

        ItemCatalog.Item item = category.findItem(itemCode);
        if (item == null) {
            throw new IllegalArgumentException("item not found in category: " + itemCode);
        }

        int styleKey = DraftServiceSupport.resolveStyleKey(draft.getConfig(), currentPage, pageNumber);
        String label = item.getItemName() == null || item.getItemName().isBlank()
                ? item.getItemCode()
                : item.getItemName();
        String newButtonId = "PresetMenuButtonMaster#NEW-" + UUID.randomUUID();

        PosConfig.Button newButton = new PosConfig.Button(
                col,
                row,
                label,
                styleKey,
                item.getItemCode(),
                item.getUnitPrice(),
                newButtonId
        );
        PosDraft.Change change = new PosDraft.AddButtonChange(pageNumber, newButton);
        String categoryName = DraftServiceSupport.resolveCategoryName(draft.getConfig(), pageNumber);
        String itemName = DraftServiceSupport.resolveItemName(item.getItemName(), item.getItemCode());
        String action = DraftServiceSupport.formatButtonAction("ボタン追加", categoryName, itemName);
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                draft,
                change,
                catalog,
                action
        );
        return updatedDraft.getConfig().getPage(pageNumber);
    }
}
