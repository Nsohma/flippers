package com.example.demo.service.port;

import com.example.demo.model.PosDraft;

import java.util.Optional;

public interface DraftRepository {
    void save(PosDraft draft);
    Optional<PosDraft> findById(String draftId);
}
