package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

@Service
public class GetHandyCatalogService implements GetHandyCatalogUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public GetHandyCatalogService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemCatalog getHandyCatalog(String draftId) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        return DraftServiceSupport.loadHandyCatalog(draft, reader, draftRepository);
    }
}
