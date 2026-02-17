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
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCategoryGridServiceTest {

    @Test
    void updateCategoryGrid_updates_grid_and_records_history() {
        FakeDraftRepository repository = new FakeDraftRepository();
        UpdateCategoryGridService service = new UpdateCategoryGridService(repository);

        PosDraft draft = new PosDraft("dft_test", initialConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        PosDraft updated = service.updateCategoryGrid("dft_test", 1, 4, 5);
        PosConfig.Page page = updated.getConfig().getPage(1);
        PosConfig.Category category = updated.getConfig().getCategories().get(0);

        assertEquals(4, page.getCols());
        assertEquals(5, page.getRows());
        assertEquals(4, category.getCols());
        assertEquals(5, category.getRows());

        String action = updated.getHistoryEntries().get(updated.getHistoryIndex()).getAction();
        assertTrue(action.startsWith("グリッド変更"), "history action should start with グリッド変更");
        assertTrue(action.contains("(PAGE1、2x2 -> 4x5)"), "history action should include category and size");
    }

    private static PosConfig initialConfig() {
        PosConfig.Category category = new PosConfig.Category(1, 2, 2, "PAGE1", 1);
        PosConfig.Button button = new PosConfig.Button(1, 1, "Coffee", 1, "ITEM01", "100", "BTN-1");
        PosConfig.Page page = new PosConfig.Page(1, 2, 2, List.of(button));
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
