package com.example.demo.service.port;

import com.example.demo.model.PosDraft;

public interface DraftRepository {
    void save(PosDraft draft);
    PosDraft load(String draftId);
}
