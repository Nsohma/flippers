package com.example.demo.service;

import com.example.demo.model.ItemMasterCatalog;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

@Service
public class GetItemMasterCatalogService implements GetItemMasterCatalogUseCase {
    private final DraftRepository draftRepository;
    private final PosConfigReader reader;

    public GetItemMasterCatalogService(DraftRepository draftRepository, PosConfigReader reader) {
        this.draftRepository = draftRepository;
        this.reader = reader;
    }

    @Override
    public ItemMasterCatalog getItemMasterCatalog(String draftId) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        return DraftServiceSupport.loadItemMasterCatalog(draft, reader, draftRepository);
    }
}
