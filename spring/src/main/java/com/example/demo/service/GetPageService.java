package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.exception.NotFoundException;
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
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));
        PosConfig.Page page = draft.getConfig().getPage(pageNumber);
        if (page == null) throw new NotFoundException("page not found: " + pageNumber);
        return page;
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
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));

        PosConfig.Page currentPage = draft.getConfig().getPage(pageNumber);
        if (currentPage == null) throw new NotFoundException("page not found: " + pageNumber);

        PosConfig updatedConfig = draft.getConfig().swapButtons(pageNumber, fromCol, fromRow, toCol, toRow);
        PosDraft updatedDraft = new PosDraft(draftId, updatedConfig, draft.getOriginalExcelBytes());
        draftRepository.save(updatedDraft);
        return updatedConfig.getPage(pageNumber);
    }
}
