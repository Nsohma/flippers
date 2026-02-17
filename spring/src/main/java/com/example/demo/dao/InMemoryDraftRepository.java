package com.example.demo.dao;

import com.example.demo.model.PosDraft;
import org.springframework.stereotype.Repository;
import com.example.demo.service.port.DraftRepository;

import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDraftRepository implements DraftRepository {
    private final ConcurrentHashMap<String, PosDraft> store = new ConcurrentHashMap<>();

    @Override
    public void save(PosDraft draft) {
        store.put(draft.getDraftId(), draft);
    }

    @Override
    public PosDraft load(String draftId) {
        PosDraft d = store.get(draftId);
        if (d == null) throw new IllegalArgumentException("draft not found: " + draftId);
        return d;
    }
}
