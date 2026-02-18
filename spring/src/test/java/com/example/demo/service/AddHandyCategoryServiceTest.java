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

class AddHandyCategoryServiceTest {

    @Test
    void add_appends_category_records_history_and_is_undoable() {
        FakeDraftRepository repository = new FakeDraftRepository();
        FakeReader reader = new FakeReader();
        AddHandyCategoryService service = new AddHandyCategoryService(repository, reader);

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "CAT-1",
                        "Category 1",
                        List.of(new ItemCatalog.Item("A", "A", "100"))
                )
        ));
        reader.source = new PosConfigSource(List.of(), List.of(), ItemCatalog.empty(), handyCatalog);

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1});
        repository.save(draft);

        ItemCatalog updated = service.add("dft_test", "Category 2");
        assertEquals(2, updated.getCategories().size());
        assertEquals("10", updated.getCategories().get(1).getCode());
        assertEquals("Category 2", updated.getCategories().get(1).getDescription());
        assertEquals(0, updated.getCategories().get(1).getItems().size());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertTrue(persisted.canUndo(), "add category should be recorded in history");
        assertEquals(2, persisted.getHistoryEntries().size());
        assertTrue(
                persisted.getHistoryEntries().get(1).getAction().startsWith("ハンディカテゴリ追加"),
                "history action should start with ハンディカテゴリ追加"
        );

        PosDraft undone = persisted.undo();
        assertEquals(1, undone.getHandyCatalogOrNull().getCategories().size());
        assertEquals("CAT-1", undone.getHandyCatalogOrNull().getCategories().get(0).getCode());
    }

    @Test
    void add_uses_smallest_unused_numeric_category_code_from_10() {
        FakeDraftRepository repository = new FakeDraftRepository();
        FakeReader reader = new FakeReader();
        AddHandyCategoryService service = new AddHandyCategoryService(repository, reader);

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("10", "Ten", List.of()),
                new ItemCatalog.Category("11", "Eleven", List.of()),
                new ItemCatalog.Category("13", "Thirteen", List.of()),
                new ItemCatalog.Category("ABC", "Other", List.of())
        ));
        reader.source = new PosConfigSource(List.of(), List.of(), ItemCatalog.empty(), handyCatalog);

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1});
        repository.save(draft);

        ItemCatalog updated = service.add("dft_test", "Auto Category");
        assertEquals("12", updated.getCategories().get(updated.getCategories().size() - 1).getCode());
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
