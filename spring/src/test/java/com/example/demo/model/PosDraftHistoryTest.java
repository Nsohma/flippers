package com.example.demo.model;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosDraftHistoryTest {

    @Test
    void undo_and_redo_restore_config() {
        PosConfig initial = configWithLabel("A");
        PosConfig updated = configWithLabel("B");

        PosDraft draft = new PosDraft("dft_test", initial, new byte[]{1});
        PosDraft afterUpdate = draft.applyNewConfig(updated, "ボタン入れ替え");

        assertTrue(afterUpdate.canUndo());
        assertFalse(afterUpdate.canRedo());
        assertEquals("B", labelOf(afterUpdate.getConfig()));
        assertEquals(2, afterUpdate.getHistoryEntries().size());
        assertEquals("インポート", afterUpdate.getHistoryEntries().get(0).getAction());
        assertEquals("ボタン入れ替え", afterUpdate.getHistoryEntries().get(1).getAction());
        assertNotNull(afterUpdate.getHistoryEntries().get(1).getTimestamp());

        PosDraft undone = afterUpdate.undo();
        assertFalse(undone.canUndo());
        assertTrue(undone.canRedo());
        assertEquals("A", labelOf(undone.getConfig()));
        assertEquals(0, undone.getHistoryIndex());

        PosDraft redone = undone.redo();
        assertTrue(redone.canUndo());
        assertFalse(redone.canRedo());
        assertEquals("B", labelOf(redone.getConfig()));
        assertEquals(1, redone.getHistoryIndex());
    }

    @Test
    void apply_new_config_after_undo_drops_redo_history() {
        PosConfig initial = configWithLabel("A");
        PosConfig second = configWithLabel("B");
        PosConfig third = configWithLabel("C");
        PosConfig branch = configWithLabel("D");

        PosDraft draft = new PosDraft("dft_test", initial, new byte[]{1})
                .applyNewConfig(second, "追加")
                .applyNewConfig(third, "削除");

        PosDraft undone = draft.undo();
        assertTrue(undone.canRedo());
        assertEquals("B", labelOf(undone.getConfig()));

        PosDraft branched = undone.applyNewConfig(branch, "価格変更");
        assertTrue(branched.canUndo());
        assertFalse(branched.canRedo());
        assertEquals("D", labelOf(branched.getConfig()));
        assertEquals(3, branched.getHistoryEntries().size());
        assertEquals("価格変更", branched.getHistoryEntries().get(2).getAction());
    }

    @Test
    void jump_to_history_index_moves_current_state() {
        PosConfig initial = configWithLabel("A");
        PosConfig second = configWithLabel("B");
        PosConfig third = configWithLabel("C");

        PosDraft draft = new PosDraft("dft_test", initial, new byte[]{1})
                .applyNewConfig(second, "追加")
                .applyNewConfig(third, "削除");

        PosDraft jumped = draft.jumpToHistoryIndex(1);
        assertEquals("B", labelOf(jumped.getConfig()));
        assertTrue(jumped.canUndo());
        assertTrue(jumped.canRedo());
        assertEquals(1, jumped.getHistoryIndex());
    }

    @Test
    void clear_history_resets_entries_at_current_state() {
        PosConfig initial = configWithLabel("A");
        PosConfig second = configWithLabel("B");
        PosConfig third = configWithLabel("C");

        PosDraft draft = new PosDraft("dft_test", initial, new byte[]{1})
                .applyNewConfig(second, "追加")
                .applyNewConfig(third, "削除")
                .jumpToHistoryIndex(1);

        PosDraft cleared = draft.clearHistory();

        assertEquals("B", labelOf(cleared.getConfig()));
        assertEquals(1, cleared.getHistoryEntries().size());
        assertEquals("履歴削除", cleared.getHistoryEntries().get(0).getAction());
        assertEquals(0, cleared.getHistoryIndex());
        assertFalse(cleared.canUndo());
        assertFalse(cleared.canRedo());
    }

    @Test
    void serialization_roundtrip_preserves_diff_history() throws Exception {
        PosConfig initial = configWithLabel("A");
        PosConfig second = configWithLabel("B");
        PosConfig third = configWithLabel("C");

        PosDraft draft = new PosDraft("dft_test", initial, new byte[]{1})
                .applyNewConfig(second, "追加")
                .applyNewConfig(third, "削除");
        assertTrue(draft.canUndo());
        assertEquals(2, draft.getHistoryIndex());
        assertEquals(3, draft.getHistoryEntries().size());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(draft);
        }

        PosDraft restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (PosDraft) in.readObject();
        }

        assertTrue(restored.canUndo());
        assertFalse(restored.canRedo());
        assertEquals(2, restored.getHistoryIndex());
        assertEquals(3, restored.getHistoryEntries().size());
        assertEquals("C", labelOf(restored.getConfig()));

        PosDraft undone = restored.undo();
        assertEquals("B", labelOf(undone.getConfig()));
        assertEquals(1, undone.getHistoryIndex());
    }

    @Test
    void swap_change_undo_restores_when_target_cell_was_empty() {
        PosConfig.Category category = new PosConfig.Category(1, 2, 1, "PAGE", 1);
        PosConfig.Button button = new PosConfig.Button(1, 1, "A", 1, "ITEM01", "100", "BTN01");
        PosConfig.Page page = new PosConfig.Page(1, 2, 1, List.of(button));
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page);

        PosDraft draft = new PosDraft("dft_test", new PosConfig(List.of(category), pages), new byte[]{1});
        PosDraft swapped = draft.applyChange(new PosDraft.SwapButtonsChange(1, 1, 1, 2, 1), "ボタン入れ替え");
        PosDraft undone = swapped.undo();

        PosConfig.Button restored = undone.getConfig().getPage(1).getButtons().get(0);
        assertEquals(1, restored.getCol());
        assertEquals(1, restored.getRow());
    }

    @Test
    void reorder_handy_items_change_is_undoable() {
        PosConfig initial = configWithLabel("A");
        ItemCatalog handyCatalog = new ItemCatalog(List.of(
                new ItemCatalog.Category(
                        "HCAT",
                        "Handy Category",
                        List.of(
                                new ItemCatalog.Item("A", "A", "100"),
                                new ItemCatalog.Item("B", "B", "200"),
                                new ItemCatalog.Item("C", "C", "300")
                        )
                )
        ));

        PosDraft draft = new PosDraft("dft_test", initial, new byte[]{1}, null, handyCatalog);
        PosDraft changed = draft.applyChange(new PosDraft.ReorderHandyItemsChange("HCAT", 0, 2), "ハンディ商品並び替え");
        assertEquals(List.of("B", "C", "A"), handyItemCodes(changed));

        PosDraft undone = changed.undo();
        assertEquals(List.of("A", "B", "C"), handyItemCodes(undone));

        PosDraft redone = undone.redo();
        assertEquals(List.of("B", "C", "A"), handyItemCodes(redone));
    }

    private static PosConfig configWithLabel(String label) {
        PosConfig.Category category = new PosConfig.Category(1, 1, 1, "PAGE", 1);
        PosConfig.Button button = new PosConfig.Button(1, 1, label, 1, "ITEM01", "100", "BTN01");
        PosConfig.Page page = new PosConfig.Page(1, 1, 1, List.of(button));
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page);
        return new PosConfig(List.of(category), pages);
    }

    private static String labelOf(PosConfig config) {
        PosConfig.Page page = config.getPage(1);
        return page.getButtons().get(0).getLabel();
    }

    private static List<String> handyItemCodes(PosDraft draft) {
        ItemCatalog handyCatalog = draft.getHandyCatalogOrNull();
        assertNotNull(handyCatalog);
        ItemCatalog.Category category = handyCatalog.getCategories().get(0);
        return category.getItems().stream()
                .map(ItemCatalog.Item::getItemCode)
                .toList();
    }
}
