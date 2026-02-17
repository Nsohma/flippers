package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteButtonService implements DeleteButtonUseCase {

    private final DraftRepository draftRepository;

    public DeleteButtonService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosConfig.Page deleteButton(String draftId, int pageNumber, String buttonId) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        PosConfig.Page currentPage = DraftServiceSupport.requirePage(draft, pageNumber);
        PosConfig.Button targetButton = DraftServiceSupport.requireButton(currentPage, buttonId);

        PosConfig updatedConfig = draft.getConfig().deleteButton(pageNumber, buttonId);
        String categoryName = DraftServiceSupport.resolveCategoryName(draft.getConfig(), pageNumber);
        String buttonName = DraftServiceSupport.resolveButtonName(targetButton);
        String action = DraftServiceSupport.formatButtonAction("ボタン削除", categoryName, buttonName);
        DraftServiceSupport.saveUpdatedDraft(draftRepository, draft, updatedConfig, null, action);
        return updatedConfig.getPage(pageNumber);
    }
}
