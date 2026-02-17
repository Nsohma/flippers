package com.example.demo.service;

import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class JumpDraftHistoryService implements JumpDraftHistoryUseCase {

    private final DraftRepository draftRepository;

    public JumpDraftHistoryService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft jumpTo(String draftId, int historyIndex) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosDraft updatedDraft = draft.jumpToHistoryIndex(historyIndex);
        draftRepository.save(updatedDraft);
        return updatedDraft;
    }
}
