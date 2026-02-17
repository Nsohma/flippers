package com.example.demo;

import com.example.demo.dao.PoiPosConfigExporter;
import com.example.demo.dao.PoiPosConfigReader;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosExportRoundTripTest {

    private static final Path ORIGINAL = Path.of("..", "2026ウインターフェア.xlsx");

    @Test
    void export_without_edits_keeps_pos_item_master_b125() throws Exception {
        byte[] originalBytes = Files.readAllBytes(ORIGINAL);

        PoiPosConfigReader reader = new PoiPosConfigReader();
        PosConfigSource source = reader.read(new ByteArrayInputStream(originalBytes));
        PosConfig config = PosConfig.fromSource(source);

        PoiPosConfigExporter exporter = new PoiPosConfigExporter();
        byte[] exported = exporter.export(originalBytes, config);

        String before = readNumericCell(originalBytes, "POSItemMaster", 125, 2); // B125
        String after = readNumericCell(exported, "POSItemMaster", 125, 2); // B125
        assertEquals(before, after, "POSItemMaster!B125 changed");
    }

    private static String readNumericCell(byte[] bytes, String sheetName, int rowNo, int colNo) throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) return null;
            var row = sheet.getRow(rowNo - 1);
            if (row == null) return null;
            var cell = row.getCell(colNo - 1);
            if (cell == null) return null;
            return switch (cell.getCellType()) {
                case NUMERIC -> Double.toString(cell.getNumericCellValue());
                case STRING -> cell.getStringCellValue();
                default -> cell.toString();
            };
        }
    }
}
