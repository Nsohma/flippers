package com.example.demo.service;

import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class UndoDraftService implements UndoDraftUseCase {

    private final DraftRepository draftRepository;

    public UndoDraftService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft undo(String draftId) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosDraft updatedDraft = draft.undo();
        draftRepository.save(updatedDraft);
        return updatedDraft;
    }
}
