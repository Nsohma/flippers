package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClearDraftHistoryServiceTest {

    @Test
    void clearHistory_resets_entries_and_disables_undo_redo() {
        FakeDraftRepository repository = new FakeDraftRepository();
        ClearDraftHistoryService service = new ClearDraftHistoryService(repository);

        PosDraft draft = new PosDraft("dft_test", initialConfig("A"), new byte[]{1, 2, 3})
                .applyNewConfig(initialConfig("B"), "ボタン追加");
        repository.save(draft);

        PosDraft cleared = service.clear("dft_test");

        assertEquals("B", cleared.getConfig().getPage(1).getButtons().get(0).getLabel());
        assertEquals(1, cleared.getHistoryEntries().size());
        assertEquals("履歴削除", cleared.getHistoryEntries().get(0).getAction());
        assertEquals(0, cleared.getHistoryIndex());
        assertFalse(cleared.canUndo());
        assertFalse(cleared.canRedo());
    }

    private static PosConfig initialConfig(String label) {
        PosConfig.Category category = new PosConfig.Category(1, 1, 1, "PAGE1", 1);
        PosConfig.Button button = new PosConfig.Button(1, 1, label, 1, "ITEM01", "100", "BTN-1");
        PosConfig.Page page = new PosConfig.Page(1, 1, 1, List.of(button));
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page);
        return new PosConfig(List.of(category), pages);
    }

    private static class FakeDraftRepository implements DraftRepository {
        private final Map<String, PosDraft> store = new HashMap<>();

        @Override
        public void save(PosDraft draft) {
            store.put(draft.getDraftId(), draft);
        }

        @Override
        public Optional<PosDraft> findById(String draftId) {
            return Optional.ofNullable(store.get(draftId));
        }
    }
}
