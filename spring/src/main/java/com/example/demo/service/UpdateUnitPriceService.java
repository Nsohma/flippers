package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateUnitPriceService implements UpdateUnitPriceUseCase {

    private final DraftRepository draftRepository;

    public UpdateUnitPriceService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosConfig.Page updateUnitPrice(String draftId, int pageNumber, String buttonId, String unitPrice) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosConfig.Page currentPage = DraftServiceSupport.requirePage(draft, pageNumber);
        PosConfig.Button targetButton = DraftServiceSupport.requireButton(currentPage, buttonId);

        String normalizedUnitPrice = DraftServiceSupport.normalizeUnitPrice(unitPrice);
        PosDraft.Change change = new PosDraft.UpdateUnitPriceChange(
                pageNumber,
                buttonId,
                targetButton.getUnitPrice(),
                normalizedUnitPrice
        );
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                draft,
                change,
                "価格変更"
        );
        return updatedDraft.getConfig().getPage(pageNumber);
    }
}
