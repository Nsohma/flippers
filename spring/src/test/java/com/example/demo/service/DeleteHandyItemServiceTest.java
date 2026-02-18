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

class DeleteHandyItemServiceTest {

    @Test
    void delete_removes_item_records_history_and_is_undoable() {
        FakeDraftRepository repository = new FakeDraftRepository();
        DeleteHandyItemService service = new DeleteHandyItemService(repository, new NoopReader());

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "CAT-1",
                        "Category 1",
                        List.of(
                                new ItemCatalog.Item("A", "A", "100"),
                                new ItemCatalog.Item("B", "B", "200"),
                                new ItemCatalog.Item("C", "C", "300")
                        )
                )
        ));
        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1}, null, handyCatalog);
        repository.save(draft);

        ItemCatalog updated = service.delete("dft_test", "CAT-1", 1);
        List<ItemCatalog.Item> items = updated.getCategories().get(0).getItems();
        assertEquals("A", items.get(0).getItemCode());
        assertEquals("C", items.get(1).getItemCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertTrue(persisted.canUndo(), "delete should be recorded in history");
        assertEquals(2, persisted.getHistoryEntries().size());
        assertTrue(
                persisted.getHistoryEntries().get(1).getAction().startsWith("ハンディ商品削除"),
                "history action should start with ハンディ商品削除"
        );

        PosDraft undone = persisted.undo();
        List<ItemCatalog.Item> undoneItems = undone.getHandyCatalogOrNull().getCategories().get(0).getItems();
        assertEquals("A", undoneItems.get(0).getItemCode());
        assertEquals("B", undoneItems.get(1).getItemCode());
        assertEquals("C", undoneItems.get(2).getItemCode());
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
