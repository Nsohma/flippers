package com.example.demo.controller;

import com.example.demo.model.PosModel;
import com.example.demo.service.ExcelPosImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pos")
public class PosImportController {

    private final ExcelPosImportService excelPosImportService;

    public PosImportController(ExcelPosImportService excelPosImportService) {
        this.excelPosImportService = excelPosImportService;
    }

    /**
     * Excelをアップロードして、PresetMenuMaster / PresetMenuButtonMaster / ItemMaster を読み込む
     * form-data key: "file"
     */
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PosModel importExcel(@RequestPart("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        return excelPosImportService.importFromExcel(file.getInputStream());
    }
}
