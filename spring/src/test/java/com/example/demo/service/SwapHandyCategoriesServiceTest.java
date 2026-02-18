package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwapHandyCategoriesServiceTest {

    @Test
    void swap_swaps_category_order_records_history_and_is_undoable() {
        FakeDraftRepository repository = new FakeDraftRepository();
        SwapHandyCategoriesService service = new SwapHandyCategoriesService(repository, new NoopReader());

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("CAT-A", "Category A", List.of()),
                new ItemCatalog.Category("CAT-B", "Category B", List.of()),
                new ItemCatalog.Category("CAT-C", "Category C", List.of())
        ));
        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1}, null, handyCatalog);
        repository.save(draft);

        ItemCatalog updated = service.swap("dft_test", "CAT-A", "CAT-C");
        List<ItemCatalog.Category> categories = updated.getCategories();
        assertEquals("CAT-C", categories.get(0).getCode());
        assertEquals("CAT-B", categories.get(1).getCode());
        assertEquals("CAT-A", categories.get(2).getCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertTrue(persisted.canUndo(), "swap should be recorded in history");
        assertEquals(2, persisted.getHistoryEntries().size());
        assertTrue(
                persisted.getHistoryEntries().get(1).getAction().startsWith("ハンディカテゴリ入れ替え"),
                "history action should start with ハンディカテゴリ入れ替え"
        );

        PosDraft undone = persisted.undo();
        List<ItemCatalog.Category> undoneCategories = undone.getHandyCatalogOrNull().getCategories();
        assertEquals("CAT-A", undoneCategories.get(0).getCode());
        assertEquals("CAT-B", undoneCategories.get(1).getCode());
        assertEquals("CAT-C", undoneCategories.get(2).getCode());
    }

    private static PosConfig emptyConfig() {
        return new PosConfig(List.of(), new LinkedHashMap<>());
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

    private static class NoopReader implements PosConfigReader {
        @Override
        public com.example.demo.model.PosConfigSource read(java.io.InputStream in) {
            throw new UnsupportedOperationException("not needed for this test");
        }
    }
}
