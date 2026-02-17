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

class SwapCategoriesServiceTest {

    @Test
    void swapCategories_swaps_and_records_history() {
        FakeDraftRepository repository = new FakeDraftRepository();
        SwapCategoriesService service = new SwapCategoriesService(repository);

        PosDraft draft = new PosDraft("dft_test", initialConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        PosDraft updated = service.swapCategories("dft_test", 1, 2);
        PosConfig.Category page1 = updated.getConfig().getCategories().stream()
                .filter(c -> c.getPageNumber() == 1)
                .findFirst()
                .orElseThrow();
        PosConfig.Category page2 = updated.getConfig().getCategories().stream()
                .filter(c -> c.getPageNumber() == 2)
                .findFirst()
                .orElseThrow();

        assertEquals("CAT-B", page1.getName());
        assertEquals("CAT-A", page2.getName());
        assertEquals("B1", updated.getConfig().getPage(1).getButtons().get(0).getLabel());
        assertEquals("A1", updated.getConfig().getPage(2).getButtons().get(0).getLabel());

        String action = updated.getHistoryEntries().get(updated.getHistoryIndex()).getAction();
        assertTrue(action.startsWith("カテゴリ入れ替え"), "history action should start with カテゴリ入れ替え");
        assertTrue(action.contains("(CAT-A <-> CAT-B)"), "history action should include swapped category names");
    }

    private static PosConfig initialConfig() {
        PosConfig.Category category1 = new PosConfig.Category(1, 2, 1, "CAT-A", 1);
        PosConfig.Category category2 = new PosConfig.Category(2, 2, 1, "CAT-B", 1);
        PosConfig.Page page1 = new PosConfig.Page(
                1,
                2,
                1,
                List.of(new PosConfig.Button(1, 1, "A1", 1, "ITEM-A", "100", "BTN-A"))
        );
        PosConfig.Page page2 = new PosConfig.Page(
                2,
                2,
                1,
                List.of(new PosConfig.Button(1, 1, "B1", 1, "ITEM-B", "200", "BTN-B"))
        );
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page1);
        pages.put(2, page2);
        return new PosConfig(List.of(category1, category2), pages);
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
