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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddButtonServiceTest {

    @Test
    void addButton_uses_cached_item_catalog_after_first_parse() throws Exception {
        FakeDraftRepository repository = new FakeDraftRepository();
        CountingReader reader = new CountingReader(sampleSource());
        AddButtonService service = new AddButtonService(repository, reader);

        PosDraft draft = new PosDraft("dft_test", initialConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        service.addButton("dft_test", 1, 1, 1, "CAT01", "ITEM01");
        service.addButton("dft_test", 1, 2, 1, "CAT01", "ITEM01");

        assertEquals(1, reader.readCount.get(), "ItemCatalog should be parsed only once");

        PosDraft updated = repository.findById("dft_test").orElseThrow();
        assertNotNull(updated.getItemCatalogOrNull(), "ItemCatalog should be cached in PosDraft");
        String latestAction = updated.getHistoryEntries().get(updated.getHistoryIndex()).getAction();
        assertTrue(latestAction.startsWith("ボタン追加"), "history action should start with ボタン追加");
        assertTrue(latestAction.contains("(PAGE1、Item 01)"), "history action should include category and item");
    }

    private static PosConfig initialConfig() {
        PosConfig.Category category = new PosConfig.Category(1, 2, 1, "PAGE1", 1);
        PosConfig.Page page = new PosConfig.Page(1, 2, 1, List.of());
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page);
        return new PosConfig(List.of(category), pages);
    }

    private static PosConfigSource sampleSource() {
        ItemCatalog catalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "CAT01",
                        "Category 01",
                        List.of(new ItemCatalog.Item("ITEM01", "Item 01", "100"))
                )
        ));
        return new PosConfigSource(List.of(), List.of(), catalog);
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

    private static class CountingReader implements PosConfigReader {
        private final PosConfigSource source;
        private final AtomicInteger readCount = new AtomicInteger(0);

        private CountingReader(PosConfigSource source) {
            this.source = source;
        }

        @Override
        public PosConfigSource read(InputStream in) {
            readCount.incrementAndGet();
            return source;
        }
    }
}
