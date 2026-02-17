package com.example.demo.service;

import com.example.demo.model.PosDraft;

public interface ClearDraftHistoryUseCase {
    PosDraft clear(String draftId);
}
