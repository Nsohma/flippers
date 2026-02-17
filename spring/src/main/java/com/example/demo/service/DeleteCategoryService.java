package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteCategoryService implements DeleteCategoryUseCase {

    private final DraftRepository draftRepository;

    public DeleteCategoryService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosConfig deleteCategory(String draftId, int pageNumber) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosConfig updatedConfig = draft.getConfig().deleteCategory(pageNumber);
        DraftServiceSupport.saveUpdatedDraft(draftRepository, draft, updatedConfig, null, "カテゴリ削除");
        return updatedConfig;
    }
}
