package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Service;

@Service
public class SwapButtonsService implements SwapButtonsUseCase {

    private final DraftRepository draftRepository;

    public SwapButtonsService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    public PosConfig.Page swapButtons(
            String draftId,
            int pageNumber,
            int fromCol,
            int fromRow,
            int toCol,
            int toRow
    ) {
        PosDraft draft = DraftServiceSupport.requireDraft(draftRepository, draftId);
        DraftServiceSupport.requirePage(draft, pageNumber);

        PosDraft.Change change = new PosDraft.SwapButtonsChange(pageNumber, fromCol, fromRow, toCol, toRow);
        PosDraft updatedDraft = DraftServiceSupport.saveDraftWithChange(
                draftRepository,
                draft,
                change,
                "ボタン入れ替え"
        );
        return updatedDraft.getConfig().getPage(pageNumber);
    }
}
