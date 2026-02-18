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

class ReorderHandyItemsServiceTest {

    @Test
    void reorder_moves_item_inside_category_and_persists() {
        FakeDraftRepository repository = new FakeDraftRepository();
        ReorderHandyItemsService service = new ReorderHandyItemsService(repository, new NoopReader());

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

        ItemCatalog updated = service.reorder("dft_test", "CAT-1", 0, 2);

        List<ItemCatalog.Item> items = updated.getCategories().get(0).getItems();
        assertEquals("B", items.get(0).getItemCode());
        assertEquals("C", items.get(1).getItemCode());
        assertEquals("A", items.get(2).getItemCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        List<ItemCatalog.Item> persistedItems = persisted.getHandyCatalogOrNull().getCategories().get(0).getItems();
        assertEquals("B", persistedItems.get(0).getItemCode());
        assertEquals("A", persistedItems.get(2).getItemCode());
        assertTrue(persisted.canUndo(), "reorder should be recorded in history");
        assertEquals(2, persisted.getHistoryEntries().size());
        assertTrue(
                persisted.getHistoryEntries().get(1).getAction().startsWith("ハンディ商品並び替え"),
                "history action should start with ハンディ商品並び替え"
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
