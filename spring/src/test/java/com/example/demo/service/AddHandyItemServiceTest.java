package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddHandyItemServiceTest {

    @Test
    void add_inserts_item_by_source_order_records_history_and_is_undoable() {
        FakeDraftRepository repository = new FakeDraftRepository();
        FakeReader reader = new FakeReader();
        AddHandyItemService service = new AddHandyItemService(repository, reader);

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "HCAT",
                        "Handy Category",
                        List.of(
                                new ItemCatalog.Item("A", "A", "100"),
                                new ItemCatalog.Item("C", "C", "300")
                        )
                )
        ));
        ItemCatalog itemCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "SRC",
                        "Source Category",
                        List.of(
                                new ItemCatalog.Item("A", "A", "100"),
                                new ItemCatalog.Item("B", "B", "200"),
                                new ItemCatalog.Item("C", "C", "300")
                        )
                )
        ));
        reader.source = new PosConfigSource(List.of(), List.of(), itemCatalog, handyCatalog);

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1});
        repository.save(draft);

        ItemCatalog updated = service.add("dft_test", "HCAT", "SRC", "B");
        List<ItemCatalog.Item> items = updated.getCategories().get(0).getItems();
        assertEquals(3, items.size());
        assertEquals("A", items.get(0).getItemCode());
        assertEquals("B", items.get(1).getItemCode());
        assertEquals("C", items.get(2).getItemCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertTrue(persisted.canUndo(), "add should be recorded in history");
        assertEquals(2, persisted.getHistoryEntries().size());
        assertTrue(
                persisted.getHistoryEntries().get(1).getAction().startsWith("ハンディ商品追加"),
                "history action should start with ハンディ商品追加"
        );

        PosDraft undone = persisted.undo();
        List<ItemCatalog.Item> undoneItems = undone.getHandyCatalogOrNull().getCategories().get(0).getItems();
        assertEquals(2, undoneItems.size());
        assertEquals("A", undoneItems.get(0).getItemCode());
        assertEquals("C", undoneItems.get(1).getItemCode());
    }

    @Test
    void add_allows_duplicate_item_code_in_same_handy_category() {
        FakeDraftRepository repository = new FakeDraftRepository();
        FakeReader reader = new FakeReader();
        AddHandyItemService service = new AddHandyItemService(repository, reader);

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "HCAT",
                        "Handy Category",
                        List.of(
                                new ItemCatalog.Item("A", "A", "100")
                        )
                )
        ));
        ItemCatalog itemCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "SRC",
                        "Source Category",
                        List.of(
                                new ItemCatalog.Item("A", "A", "100"),
                                new ItemCatalog.Item("B", "B", "200")
                        )
                )
        ));
        reader.source = new PosConfigSource(List.of(), List.of(), itemCatalog, handyCatalog);

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1});
        repository.save(draft);

        ItemCatalog updated = service.add("dft_test", "HCAT", "SRC", "A");
        List<ItemCatalog.Item> items = updated.getCategories().get(0).getItems();
        assertEquals(2, items.size());
        assertEquals("A", items.get(0).getItemCode());
        assertEquals("A", items.get(1).getItemCode());
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

    private static class FakeReader implements PosConfigReader {
        private PosConfigSource source;

        @Override
        public PosConfigSource read(InputStream in) {
            return source;
        }
    }
}
