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
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateItemMasterServiceTest {

    @Test
    void updateItem_updates_itemMaster_and_related_catalogs_and_buttons() {
        FakeDraftRepository repository = new FakeDraftRepository();
        FakePosConfigReader reader = new FakePosConfigReader(sampleSource());
        UpdateItemMasterService service = new UpdateItemMasterService(repository, reader);

        PosDraft draft = new PosDraft("dft_test", initialConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        ItemMasterCatalog updated = service.updateItem(
                "dft_test",
                "1001",
                "1002",
                "スフレ改",
                "1300",
                "220",
                "1350"
        );

        assertEquals("1002", updated.getItems().get(0).getItemCode());
        assertEquals("スフレ改", updated.getItems().get(0).getItemNamePrint());
        assertEquals("1300", updated.getItems().get(0).getUnitPrice());
        assertEquals("220", updated.getItems().get(0).getCostPrice());
        assertEquals("1350", updated.getItems().get(0).getBasePrice());

        PosDraft persisted = repository.findById("dft_test").orElseThrow();
        PosConfig.Page page = persisted.getConfig().getPage(1);
        assertNotNull(page);
        assertEquals("1002", page.getButtons().get(0).getItemCode());
        assertEquals("1300", page.getButtons().get(0).getUnitPrice());

        ItemCatalog itemCatalog = persisted.getItemCatalogOrNull();
        assertNotNull(itemCatalog);
        ItemCatalog.Item posItem = itemCatalog.getCategories().get(0).getItems().get(0);
        assertEquals("1002", posItem.getItemCode());
        assertEquals("スフレ改", posItem.getItemName());
        assertEquals("1300", posItem.getUnitPrice());

        ItemCatalog handyCatalog = persisted.getHandyCatalogOrNull();
        assertNotNull(handyCatalog);
        ItemCatalog.Item handyItem = handyCatalog.getCategories().get(0).getItems().get(0);
        assertEquals("1002", handyItem.getItemCode());
        assertEquals("スフレ改", handyItem.getItemName());
        assertEquals("1300", handyItem.getUnitPrice());
    }

    @Test
    void updateItem_rejects_duplicate_itemCode() {
        FakeDraftRepository repository = new FakeDraftRepository();
        FakePosConfigReader reader = new FakePosConfigReader(sourceWithTwoItems());
        UpdateItemMasterService service = new UpdateItemMasterService(repository, reader);

        PosDraft draft = new PosDraft("dft_test", initialConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateItem("dft_test", "1001", "1003", "スフレ", "1300", "220", "1350")
        );
        assertEquals("itemCode already exists: 1003", ex.getMessage());
    }

    private static PosConfig initialConfig() {
        PosConfig.Category category = new PosConfig.Category(1, 3, 1, "PAGE1", 1);
        PosConfig.Button button = new PosConfig.Button(1, 1, "スフレ", 1, "1001", "1200", "btn-1");
        PosConfig.Page page = new PosConfig.Page(1, 3, 1, List.of(button));
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page);
        return new PosConfig(List.of(category), pages);
    }

    private static PosConfigSource sampleSource() {
        ItemCatalog itemCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("CAT01", "カテゴリ01", List.of(new ItemCatalog.Item("1001", "スフレ", "1200")))
        ));
        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("HCAT01", "ハンディ01", List.of(new ItemCatalog.Item("1001", "スフレ", "1200")))
        ));
        ItemMasterCatalog itemMasterCatalog = new ItemMasterCatalog(List.of(
                new ItemMasterCatalog.Item("1001", "スフレ", "1200", "200", "1200")
        ));
        return new PosConfigSource(List.of(), List.of(), itemCatalog, handyCatalog, itemMasterCatalog);
    }

    private static PosConfigSource sourceWithTwoItems() {
        ItemCatalog itemCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "CAT01",
                        "カテゴリ01",
                        List.of(
                                new ItemCatalog.Item("1001", "スフレ", "1200"),
                                new ItemCatalog.Item("1003", "パンケーキ", "1100")
                        )
                )
        ));
        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category("HCAT01", "ハンディ01", List.of(new ItemCatalog.Item("1001", "スフレ", "1200")))
        ));
        ItemMasterCatalog itemMasterCatalog = new ItemMasterCatalog(List.of(
                new ItemMasterCatalog.Item("1001", "スフレ", "1200", "200", "1200"),
                new ItemMasterCatalog.Item("1003", "パンケーキ", "1100", "180", "1100")
        ));
        return new PosConfigSource(List.of(), List.of(), itemCatalog, handyCatalog, itemMasterCatalog);
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

    private static class FakePosConfigReader implements PosConfigReader {
        private final PosConfigSource source;

        private FakePosConfigReader(PosConfigSource source) {
            this.source = source;
        }

        @Override
        public PosConfigSource read(InputStream in) {
            return source;
        }
    }
}
