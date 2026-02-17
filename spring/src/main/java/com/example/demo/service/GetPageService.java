package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class GetPageService implements GetPageUseCase {

    private final DraftRepository draftRepository;

    public GetPageService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosConfig.Page getPage(String draftId, int pageNumber) {
        PosDraft draft = draftRepository.load(draftId);
        PosConfig.Page page = draft.getConfig().getPage(pageNumber);
        if (page == null) throw new IllegalArgumentException("page not found: " + pageNumber);
        return page;
    }
}
