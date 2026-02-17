package com.example.demo.service;

import com.example.demo.model.PosDraft;

public interface GetDraftHistoryUseCase {
    PosDraft getHistory(String draftId);
}
