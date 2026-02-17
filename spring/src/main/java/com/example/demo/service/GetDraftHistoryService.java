package com.example.demo.service;

import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class GetDraftHistoryService implements GetDraftHistoryUseCase {

    private final DraftRepository draftRepository;

    public GetDraftHistoryService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft getHistory(String draftId) {
        return DraftServiceSupport.requireDraft(draftRepository, draftId);
    }
}
