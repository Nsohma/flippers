package com.example.demo.service;

import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class RedoDraftService implements RedoDraftUseCase {

    private final DraftRepository draftRepository;

    public RedoDraftService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft redo(String draftId) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosDraft updatedDraft = draft.redo();
        draftRepository.save(updatedDraft);
        return updatedDraft;
    }
}
