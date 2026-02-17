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
        DraftServiceSupport.requirePage(draft, pageNumber);

        String normalizedUnitPrice = DraftServiceSupport.normalizeUnitPrice(unitPrice);
        PosConfig updatedConfig = draft.getConfig().updateUnitPrice(pageNumber, buttonId, normalizedUnitPrice);
        DraftServiceSupport.saveUpdatedDraft(draftRepository, draft, updatedConfig, null, "価格変更");
        return updatedConfig.getPage(pageNumber);
    }
}
