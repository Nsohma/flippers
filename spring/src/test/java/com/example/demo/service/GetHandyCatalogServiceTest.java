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

class GetHandyCatalogServiceTest {

    @Test
    void getHandyCatalog_returns_handy_catalog_from_excel_source() throws Exception {
        FakeDraftRepository repository = new FakeDraftRepository();
        FakePosConfigReader reader = new FakePosConfigReader();
        GetHandyCatalogService service = new GetHandyCatalogService(repository, reader);

        PosDraft draft = new PosDraft("dft_test", emptyConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "CAT-B",
                        "カテゴリB",
                        List.of(new ItemCatalog.Item("1002", "B-2", "200"))
                ),
                new ItemCatalog.Category(
                        "CAT-A",
                        "カテゴリA",
                        List.of(new ItemCatalog.Item("0001", "A-1", "100"))
                )
        ));

        reader.source = new PosConfigSource(List.of(), List.of(), ItemCatalog.empty(), handyCatalog);

        ItemCatalog actual = service.getHandyCatalog("dft_test");
        assertEquals(2, actual.getCategories().size());
        assertEquals("CAT-B", actual.getCategories().get(0).getCode());
        assertEquals("1002", actual.getCategories().get(0).getItems().get(0).getItemCode());
        assertEquals("CAT-A", actual.getCategories().get(1).getCode());
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

    private static class FakePosConfigReader implements PosConfigReader {
        private PosConfigSource source;

        @Override
        public PosConfigSource read(InputStream in) {
            return source;
        }
    }
}
