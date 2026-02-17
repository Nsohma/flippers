package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class SwapCategoriesService implements SwapCategoriesUseCase {

    private final DraftRepository draftRepository;

    public SwapCategoriesService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft swapCategories(String draftId, int fromPageNumber, int toPageNumber) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosConfig.Category fromCategory = DraftServiceSupport.requireCategory(draft, fromPageNumber);
        PosConfig.Category toCategory = DraftServiceSupport.requireCategory(draft, toPageNumber);

        PosDraft.Change change = new PosDraft.SwapCategoriesChange(fromPageNumber, toPageNumber);
        String action = "カテゴリ入れ替え (" + fromCategory.getName() + " <-> " + toCategory.getName() + ")";
        return DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                draft,
                change,
                action
        );
    }
}
