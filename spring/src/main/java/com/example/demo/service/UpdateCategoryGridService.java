package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateCategoryGridService implements UpdateCategoryGridUseCase {

    private final DraftRepository draftRepository;

    public UpdateCategoryGridService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft updateCategoryGrid(String draftId, int pageNumber, int cols, int rows) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosConfig.Page currentPage = DraftServiceSupport.requirePage(draft, pageNumber);

        PosDraft.Change change = new PosDraft.UpdateCategoryGridChange(
                pageNumber,
                currentPage.getCols(),
                currentPage.getRows(),
                cols,
                rows
        );
        String categoryName = DraftServiceSupport.resolveCategoryName(draft.getConfig(), pageNumber);
        String action = "グリッド変更 (" + categoryName + "、"
                + currentPage.getCols() + "x" + currentPage.getRows()
                + " -> " + cols + "x" + rows + ")";

        return DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                draft,
                change,
                action
        );
    }
}
