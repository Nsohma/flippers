package com.example.demo.dao;

import com.example.demo.model.PosDraft;
import com.example.demo.service.exception.NotFoundException;
import com.example.demo.service.port.DraftRepository;
import org.springframework.stereotype.Repository;

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
        if (d == null) throw new NotFoundException("draft not found: " + draftId);
        return d;
    }
}
