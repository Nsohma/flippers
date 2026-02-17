package com.example.demo.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosConfigGridResizeTest {

    @Test
    void update_category_grid_expands_page_and_category() {
        PosConfig config = baseConfig(2, 2, List.of(
                new PosConfig.Button(2, 2, "Coffee", 1, "ITEM01", "100", "BTN-1")
        ));

        PosConfig updated = config.updateCategoryGrid(1, 4, 3);

        PosConfig.Category category = updated.getCategories().get(0);
        PosConfig.Page page = updated.getPage(1);
        assertEquals(4, category.getCols());
        assertEquals(3, category.getRows());
        assertEquals(4, page.getCols());
        assertEquals(3, page.getRows());
        assertEquals(1, page.getButtons().size());
    }

    @Test
    void update_category_grid_rejects_shrink_when_button_goes_out_of_range() {
        PosConfig config = baseConfig(3, 2, List.of(
                new PosConfig.Button(3, 2, "Coffee", 1, "ITEM01", "100", "BTN-1")
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> config.updateCategoryGrid(1, 2, 2)
        );
        assertEquals("button out of range for resized grid: (3,2)", ex.getMessage());
    }

    private static PosConfig baseConfig(int cols, int rows, List<PosConfig.Button> buttons) {
        PosConfig.Category category = new PosConfig.Category(1, cols, rows, "PAGE1", 1);
        PosConfig.Page page = new PosConfig.Page(1, cols, rows, buttons);
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page);
        return new PosConfig(List.of(category), pages);
    }
}
