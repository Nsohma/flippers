package com.example.demo;

import com.example.demo.dao.PoiPosConfigExporter;
import com.example.demo.dao.PoiPosConfigReader;
import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelGoldenRegressionTest {

    private static final Path ORIGINAL = Path.of("..", "2026ウインターフェア.xlsx");
    private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "golden");
    private static final boolean UPDATE_GOLDEN = Boolean.getBoolean("updateGolden");

    @Test
    void export_without_edits_matches_golden_snapshot() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, config, source.getHandyCatalog());

        assertOrUpdateGolden("no-edits.snap.txt", snapshotWorkbook(exported));
    }

    @Test
    void pos_edits_match_golden_snapshot() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig updatedConfig = applyPosScenario(PosConfig.fromSource(source));

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, updatedConfig, source.getHandyCatalog());

        assertOrUpdateGolden("pos-edits.snap.txt", snapshotWorkbook(exported));
    }

    @Test
    void handy_edits_match_golden_snapshot() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        ItemCatalog updatedHandyCatalog = applyHandyScenario(source.getHandyCatalog());
        PosConfig config = PosConfig.fromSource(source);

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, config, updatedHandyCatalog);

        assertOrUpdateGolden("handy-edits.snap.txt", snapshotWorkbook(exported));
    }

    private static PosConfig applyPosScenario(PosConfig config) {
        SwapCandidate candidate = findSwapCandidate(config);
        PosConfig updated = config.updateUnitPrice(
                candidate.pageNumber(),
                candidate.button().getButtonId(),
                "7777"
        );

        PosConfig.Page page = updated.getPage(candidate.pageNumber());
        assertTrue(page != null, "page not found after unit price update");

        PosConfig.Button movedButton = null;
        for (PosConfig.Button button : page.getButtons()) {
            if (candidate.button().getButtonId().equals(button.getButtonId())) {
                movedButton = button;
                break;
            }
        }
        assertTrue(movedButton != null, "target button not found after unit price update");

        return updated.swapButtons(
                candidate.pageNumber(),
                movedButton.getCol(),
                movedButton.getRow(),
                candidate.emptyCol(),
                candidate.emptyRow()
        );
    }

    private static ItemCatalog applyHandyScenario(ItemCatalog handyCatalog) {
        List<ItemCatalog.Category> categories = new ArrayList<>(handyCatalog.getCategories());
        assertTrue(categories.size() >= 2, "handy categories must contain at least 2 entries for regression scenario");

        ItemCatalog current = reorderCategories(handyCatalog, 0, 1);
        List<ItemCatalog.Category> afterCategoryReorder = current.getCategories();

        int reorderTargetCategoryIndex = -1;
        for (int i = 0; i < afterCategoryReorder.size(); i++) {
            if (afterCategoryReorder.get(i).getItems().size() >= 2) {
                reorderTargetCategoryIndex = i;
                break;
            }
        }
        assertTrue(reorderTargetCategoryIndex >= 0, "no handy category has at least 2 items");

        ItemCatalog.Category reorderTargetCategory = afterCategoryReorder.get(reorderTargetCategoryIndex);
        current = reorderItems(
                current,
                reorderTargetCategory.getCode(),
                0,
                reorderTargetCategory.getItems().size() - 1
        );

        ItemCatalog.Category deleteTargetCategory = null;
        for (ItemCatalog.Category category : current.getCategories()) {
            if (!category.getItems().isEmpty()) {
                deleteTargetCategory = category;
                break;
            }
        }
        assertTrue(deleteTargetCategory != null, "no handy category has deletable items");
        return deleteItem(current, deleteTargetCategory.getCode(), 0);
    }

    private static SwapCandidate findSwapCandidate(PosConfig config) {
        List<PosConfig.Category> categories = new ArrayList<>(config.getCategories());
        categories.sort((a, b) -> Integer.compare(a.getPageNumber(), b.getPageNumber()));

        for (PosConfig.Category category : categories) {
            PosConfig.Page page = config.getPage(category.getPageNumber());
            if (page == null) {
                continue;
            }
            if (page.getButtons().isEmpty()) {
                continue;
            }

            Set<String> occupied = new HashSet<>();
            for (PosConfig.Button button : page.getButtons()) {
                occupied.add(button.getCol() + "-" + button.getRow());
            }

            for (int row = 1; row <= page.getRows(); row++) {
                for (int col = 1; col <= page.getCols(); col++) {
                    String key = col + "-" + row;
                    if (occupied.contains(key)) {
                        continue;
                    }
                    PosConfig.Button sourceButton = page.getButtons().get(0);
                    return new SwapCandidate(page.getPageNumber(), sourceButton, col, row);
                }
            }
        }
        throw new IllegalStateException("no page has both button and empty cell for swap scenario");
    }

    private static ItemCatalog reorderCategories(ItemCatalog catalog, int fromIndex, int toIndex) {
        List<ItemCatalog.Category> categories = new ArrayList<>(catalog.getCategories());
        ItemCatalog.Category moved = categories.remove(fromIndex);
        categories.add(toIndex, moved);
        return new ItemCatalog(categories);
    }

    private static ItemCatalog reorderItems(ItemCatalog catalog, String categoryCode, int fromIndex, int toIndex) {
        List<ItemCatalog.Category> categories = new ArrayList<>(catalog.getCategories());
        int categoryIndex = findCategoryIndex(categories, categoryCode);
        ItemCatalog.Category category = categories.get(categoryIndex);

        List<ItemCatalog.Item> items = new ArrayList<>(category.getItems());
        ItemCatalog.Item moved = items.remove(fromIndex);
        items.add(toIndex, moved);

        categories.set(
                categoryIndex,
                new ItemCatalog.Category(category.getCode(), category.getDescription(), items)
        );
        return new ItemCatalog(categories);
    }

    private static ItemCatalog deleteItem(ItemCatalog catalog, String categoryCode, int itemIndex) {
        List<ItemCatalog.Category> categories = new ArrayList<>(catalog.getCategories());
        int categoryIndex = findCategoryIndex(categories, categoryCode);
        ItemCatalog.Category category = categories.get(categoryIndex);

        List<ItemCatalog.Item> items = new ArrayList<>(category.getItems());
        assertFalse(items.isEmpty(), "target category has no items to delete");
        items.remove(itemIndex);

        categories.set(
                categoryIndex,
                new ItemCatalog.Category(category.getCode(), category.getDescription(), items)
        );
        return new ItemCatalog(categories);
    }

    private static int findCategoryIndex(List<ItemCatalog.Category> categories, String categoryCode) {
        for (int i = 0; i < categories.size(); i++) {
            if (categoryCode.equals(categories.get(i).getCode())) {
                return i;
            }
        }
        throw new IllegalArgumentException("category not found: " + categoryCode);
    }

    private static void assertOrUpdateGolden(String fileName, List<String> actualLines) throws Exception {
        Path file = GOLDEN_DIR.resolve(fileName);
        if (UPDATE_GOLDEN) {
            Files.createDirectories(file.getParent());
            Files.write(file, String.join("\n", actualLines).getBytes(StandardCharsets.UTF_8));
            return;
        }

        assertTrue(Files.exists(file), "golden snapshot not found: " + file + " (run with -DupdateGolden=true)");
        List<String> expectedLines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(
                expectedLines,
                actualLines,
                () -> "golden snapshot mismatch: " + file + "\n" + diffSummary(expectedLines, actualLines)
        );
    }

    private static List<String> snapshotWorkbook(byte[] workbookBytes) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            List<String> lines = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = wb.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();
                lines.add("[" + sheetName + "]");

                int firstRowNum = Math.max(sheet.getFirstRowNum(), 0);
                int lastRowNum = sheet.getLastRowNum();
                if (lastRowNum < firstRowNum) {
                    continue;
                }
                for (int rowIndex = firstRowNum; rowIndex <= lastRowNum; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    short lastCellNum = row.getLastCellNum();
                    if (lastCellNum < 0) {
                        continue;
                    }
                    for (int colIndex = 0; colIndex < lastCellNum; colIndex++) {
                        Cell cell = row.getCell(colIndex);
                        String value = normalizeCell(cell);
                        if (value == null) {
                            continue;
                        }
                        String ref = CellReference.convertNumToColString(colIndex) + (rowIndex + 1);
                        lines.add(sheetName + "!" + ref + "=" + value);
                    }
                }
            }
            return lines;
        }
    }

    private static String normalizeCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        return switch (type) {
            case STRING -> {
                String text = cell.getStringCellValue();
                if (text == null || text.isEmpty()) {
                    yield null;
                }
                yield "S|" + text;
            }
            case NUMERIC -> "N|" + NumberToTextConverter.toText(cell.getNumericCellValue());
            case BOOLEAN -> "B|" + cell.getBooleanCellValue();
            case FORMULA -> "F|" + cell.getCellFormula();
            case ERROR -> "E|" + FormulaError.forInt(cell.getErrorCellValue()).getString();
            case BLANK -> null;
            default -> {
                String text = cell.toString();
                if (text == null || text.isEmpty()) {
                    yield null;
                }
                yield "U|" + text;
            }
        };
    }

    private static String diffSummary(List<String> expected, List<String> actual) {
        int min = Math.min(expected.size(), actual.size());
        for (int i = 0; i < min; i++) {
            String left = expected.get(i);
            String right = actual.get(i);
            if (!left.equals(right)) {
                return "first diff at line " + (i + 1)
                        + "\nexpected: " + left
                        + "\nactual  : " + right
                        + "\nexpected lines: " + expected.size()
                        + ", actual lines: " + actual.size();
            }
        }
        return "line count differs. expected lines: " + expected.size() + ", actual lines: " + actual.size();
    }

    private record SwapCandidate(
            int pageNumber,
            PosConfig.Button button,
            int emptyCol,
            int emptyRow
    ) {
    }
}
