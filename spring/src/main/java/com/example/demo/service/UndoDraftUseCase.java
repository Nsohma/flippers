package com.example.demo.service;

import com.example.demo.model.PosDraft;

public interface UndoDraftUseCase {
    PosDraft undo(String draftId);
}
