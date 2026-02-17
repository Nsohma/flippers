package com.example.demo.model;

import org.junit.jupiter.api.Test;

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
}
