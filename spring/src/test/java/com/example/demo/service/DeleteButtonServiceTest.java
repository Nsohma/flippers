package com.example.demo.service;

import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteButtonServiceTest {

    @Test
    void deleteButton_records_category_and_item_name_in_history_action() {
        FakeDraftRepository repository = new FakeDraftRepository();
        DeleteButtonService service = new DeleteButtonService(repository);

        PosDraft draft = new PosDraft("dft_test", initialConfig(), new byte[]{1, 2, 3});
        repository.save(draft);

        service.deleteButton("dft_test", 1, "BTN-1");

        PosDraft updated = repository.findById("dft_test").orElseThrow();
        assertEquals(0, updated.getConfig().getPage(1).getButtons().size(), "button should be deleted");

        String latestAction = updated.getHistoryEntries().get(updated.getHistoryIndex()).getAction();
        assertTrue(latestAction.startsWith("ボタン削除"), "history action should start with ボタン削除");
        assertTrue(latestAction.contains("(PAGE1、Coffee)"), "history action should include category and item");
    }

    private static PosConfig initialConfig() {
        PosConfig.Category category = new PosConfig.Category(1, 3, 2, "PAGE1", 1);
        PosConfig.Button button = new PosConfig.Button(1, 1, "Coffee", 1, "ITEM01", "100", "BTN-1");
        PosConfig.Page page = new PosConfig.Page(1, 3, 2, List.of(button));
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page);
        return new PosConfig(List.of(category), pages);
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
}
