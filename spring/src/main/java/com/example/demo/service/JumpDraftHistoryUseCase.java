package com.example.demo.service;

import com.example.demo.model.PosDraft;

public interface JumpDraftHistoryUseCase {
    PosDraft jumpTo(String draftId, int historyIndex);
}
