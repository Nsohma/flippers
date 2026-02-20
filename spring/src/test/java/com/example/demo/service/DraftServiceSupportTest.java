package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.ItemMasterCatalog;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DraftServiceSupportTest {

    @Test
    void loadItemThenHandy_parsesExcelOnlyOnce_andCachesBothCatalogs() {
        FakeDraftRepository repository = new FakeDraftRepository();
        CountingReader reader = new CountingReader();

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        ItemCatalog itemCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("SRC", "Source", List.of(new ItemCatalog.Item("1001", "A", "100")))
        ));
        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("HCAT", "Handy", List.of(new ItemCatalog.Item("2001", "B", "200")))
        ));
        ItemMasterCatalog itemMasterCatalog = new ItemMasterCatalog(List.of(
                new ItemMasterCatalog.Item("1001", "A", "100", "60", "90")
        ));
        reader.source = new PosConfigSource(List.of(), List.of(), itemCatalog, handyCatalog, itemMasterCatalog);

        ItemCatalog loadedItem = DraftServiceSupport.loadItemCatalog(draft, reader, repository);
        ItemCatalog loadedHandy = DraftServiceSupport.loadHandyCatalog(draft, reader, repository);

        assertEquals(1, reader.readCount, "Excel should be parsed only once");
        assertEquals("1001", loadedItem.getCategories().get(0).getItems().get(0).getItemCode());
        assertEquals("2001", loadedHandy.getCategories().get(0).getItems().get(0).getItemCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertNotNull(persisted.getItemCatalogOrNull(), "itemCatalog should be cached");
        assertNotNull(persisted.getHandyCatalogOrNull(), "handyCatalog should be cached");
        assertNotNull(persisted.getItemMasterCatalogOrNull(), "itemMasterCatalog should be cached");
    }

    @Test
    void loadHandyThenItem_parsesExcelOnlyOnce_andCachesBothCatalogs() {
        FakeDraftRepository repository = new FakeDraftRepository();
        CountingReader reader = new CountingReader();

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        ItemCatalog itemCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("SRC", "Source", List.of(new ItemCatalog.Item("1001", "A", "100")))
        ));
        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("HCAT", "Handy", List.of(new ItemCatalog.Item("2001", "B", "200")))
        ));
        ItemMasterCatalog itemMasterCatalog = new ItemMasterCatalog(List.of(
                new ItemMasterCatalog.Item("1001", "A", "100", "60", "90")
        ));
        reader.source = new PosConfigSource(List.of(), List.of(), itemCatalog, handyCatalog, itemMasterCatalog);

        ItemCatalog loadedHandy = DraftServiceSupport.loadHandyCatalog(draft, reader, repository);
        ItemCatalog loadedItem = DraftServiceSupport.loadItemCatalog(draft, reader, repository);

        assertEquals(1, reader.readCount, "Excel should be parsed only once");
        assertEquals("2001", loadedHandy.getCategories().get(0).getItems().get(0).getItemCode());
        assertEquals("1001", loadedItem.getCategories().get(0).getItems().get(0).getItemCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertNotNull(persisted.getItemCatalogOrNull(), "itemCatalog should be cached");
        assertNotNull(persisted.getHandyCatalogOrNull(), "handyCatalog should be cached");
        assertNotNull(persisted.getItemMasterCatalogOrNull(), "itemMasterCatalog should be cached");
    }

    @Test
    void loadItemMasterThenItem_parsesExcelOnlyOnce_andCachesAllCatalogs() {
        FakeDraftRepository repository = new FakeDraftRepository();
        CountingReader reader = new CountingReader();

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        ItemCatalog itemCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("SRC", "Source", List.of(new ItemCatalog.Item("1001", "A", "100")))
        ));
        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("HCAT", "Handy", List.of(new ItemCatalog.Item("2001", "B", "200")))
        ));
        ItemMasterCatalog itemMasterCatalog = new ItemMasterCatalog(List.of(
                new ItemMasterCatalog.Item("1001", "A", "100", "60", "90")
        ));
        reader.source = new PosConfigSource(List.of(), List.of(), itemCatalog, handyCatalog, itemMasterCatalog);

        ItemMasterCatalog loadedItemMaster = DraftServiceSupport.loadItemMasterCatalog(draft, reader, repository);
        ItemCatalog loadedItem = DraftServiceSupport.loadItemCatalog(draft, reader, repository);

        assertEquals(1, reader.readCount, "Excel should be parsed only once");
        assertEquals("1001", loadedItemMaster.getItems().get(0).getItemCode());
        assertEquals("1001", loadedItem.getCategories().get(0).getItems().get(0).getItemCode());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        assertNotNull(persisted.getItemCatalogOrNull(), "itemCatalog should be cached");
        assertNotNull(persisted.getHandyCatalogOrNull(), "handyCatalog should be cached");
        assertNotNull(persisted.getItemMasterCatalogOrNull(), "itemMasterCatalog should be cached");
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

    private static class CountingReader implements PosConfigReader {
        private PosConfigSource source;
        private int readCount;

        @Override
        public PosConfigSource read(InputStream in) {
            readCount += 1;
            return source;
        }
    }
}
