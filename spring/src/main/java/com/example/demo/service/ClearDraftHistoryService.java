package com.example.demo.service;

import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class ClearDraftHistoryService implements ClearDraftHistoryUseCase {

    private final DraftRepository draftRepository;

    public ClearDraftHistoryService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft clear(String draftId) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosDraft updatedDraft = draft.clearHistory();
        draftRepository.save(updatedDraft);
        return updatedDraft;
    }
}
