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
        PosConfig.Category targetCategory = DraftServiceSupport.requireCategory(draft, pageNumber);
        PosConfig.Page targetPage = DraftServiceSupport.requirePage(draft, pageNumber);

        PosDraft.Change change = new PosDraft.DeleteCategoryChange(targetCategory, targetPage);
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                draft,
                change,
                "カテゴリ削除"
        );
        return updatedDraft.getConfig();
    }
}
