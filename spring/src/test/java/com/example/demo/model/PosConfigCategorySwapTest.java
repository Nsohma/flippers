package com.example.demo.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosConfigCategorySwapTest {

    @Test
    void swap_categories_swaps_category_meta_and_page_contents() {
        PosConfig.Category category1 = new PosConfig.Category(1, 2, 1, "CAT-A", 11);
        PosConfig.Category category2 = new PosConfig.Category(2, 3, 2, "CAT-B", 22);
        PosConfig.Page page1 = new PosConfig.Page(
                1,
                2,
                1,
                List.of(new PosConfig.Button(1, 1, "A1", 1, "ITEM-A", "100", "BTN-A"))
        );
        PosConfig.Page page2 = new PosConfig.Page(
                2,
                3,
                2,
                List.of(new PosConfig.Button(2, 2, "B1", 2, "ITEM-B", "200", "BTN-B"))
        );
        Map<Integer, PosConfig.Page> pages = new LinkedHashMap<>();
        pages.put(1, page1);
        pages.put(2, page2);
        PosConfig config = new PosConfig(List.of(category1, category2), pages);

        PosConfig swapped = config.swapCategories(1, 2);

        PosConfig.Category atPage1 = swapped.getCategories().stream()
                .filter(c -> c.getPageNumber() == 1)
                .findFirst()
                .orElseThrow();
        PosConfig.Category atPage2 = swapped.getCategories().stream()
                .filter(c -> c.getPageNumber() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals("CAT-B", atPage1.getName());
        assertEquals(3, atPage1.getCols());
        assertEquals(2, atPage1.getRows());
        assertEquals(22, atPage1.getStyleKey());

        assertEquals("CAT-A", atPage2.getName());
        assertEquals(2, atPage2.getCols());
        assertEquals(1, atPage2.getRows());
        assertEquals(11, atPage2.getStyleKey());

        PosConfig.Page swappedPage1 = swapped.getPage(1);
        PosConfig.Page swappedPage2 = swapped.getPage(2);
        assertEquals(1, swappedPage1.getPageNumber());
        assertEquals(2, swappedPage2.getPageNumber());
        assertEquals("B1", swappedPage1.getButtons().get(0).getLabel());
        assertEquals("A1", swappedPage2.getButtons().get(0).getLabel());
    }
}
