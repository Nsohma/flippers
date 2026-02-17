package com.example.demo.service;

import com.example.demo.model.PosModel;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.stream.IntStream;

@Service
public class ExcelPosImportService {

    public PosModel importFromExcel(InputStream in) throws Exception {
        try (Workbook wb = new XSSFWorkbook(in)) {
            var sheets = IntStream.range(0, wb.getNumberOfSheets())
                    .mapToObj(wb::getSheetName)
                    .toList();

            // まずは疎通用に sheet名だけ返す（本実装ではここで3シート解析に置き換える）
            return PosModel.forDebug(sheets);
        }
    }
}
