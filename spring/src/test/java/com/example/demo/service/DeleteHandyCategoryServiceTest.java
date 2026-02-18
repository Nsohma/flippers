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

class DeleteHandyCategoryServiceTest {

    @Test
    void delete_removes_category_records_history_and_is_undoable() {
        FakeDraftRepository repository = new FakeDraftRepository();
        DeleteHandyCategoryService service = new DeleteHandyCategoryService(repository, new NoopReader());

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "CAT-1",
                        "Category 1",
                        List.of(new ItemCatalog.Item("A", "A", "100"))
                ),
                new ItemCatalog.Category(
                        "CAT-2",
                        "Category 2",
                        List.of(new ItemCatalog.Item("B", "B", "200"))
                )
        ));
        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1}, null, handyCatalog);
        repository.save(draft);

        ItemCatalog updated = service.delete("dft_test", "CAT-1");
        assertEquals(1, updated.getCategories().size());
        assertEquals("CAT-2", updated.getCategories().get(0).getCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertTrue(persisted.canUndo(), "delete category should be recorded in history");
        assertEquals(2, persisted.getHistoryEntries().size());
        assertTrue(
                persisted.getHistoryEntries().get(1).getAction().startsWith("ハンディカテゴリ削除"),
                "history action should start with ハンディカテゴリ削除"
        );

        PosDraft undone = persisted.undo();
        assertEquals(2, undone.getHandyCatalogOrNull().getCategories().size());
        assertEquals("CAT-1", undone.getHandyCatalogOrNull().getCategories().get(0).getCode());
        assertEquals("CAT-2", undone.getHandyCatalogOrNull().getCategories().get(1).getCode());
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
