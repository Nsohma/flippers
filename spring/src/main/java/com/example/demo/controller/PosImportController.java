package com.example.demo.controller;

import com.example.demo.controller.dto.ImportResponse;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.ImportPosUseCase;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

//エクセルのImportを受け取るコントローラ
@RestController
@RequestMapping("/api/pos")
public class PosImportController {

    private final ImportPosUseCase importUseCase;

    public PosImportController(ImportPosUseCase importUseCase) {
        this.importUseCase = importUseCase;
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ImportResponse importExcel(@RequestPart("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("file is empty");

        PosDraft draft = importUseCase.importExcel(file.getInputStream());
        PosConfig config = draft.getConfig();

        ImportResponse res = new ImportResponse();
        res.draftId = draft.getDraftId();

        res.categories = config.getCategories().stream().map(c -> {
            ImportResponse.CategoryDto dto = new ImportResponse.CategoryDto();
            dto.pageNumber = c.getPageNumber();
            dto.cols = c.getCols();
            dto.rows = c.getRows();
            dto.name = c.getName();
            dto.styleKey = c.getStyleKey();
            return dto;
        }).collect(Collectors.toList());

        PosConfig.Category first = config.firstCategoryOrNull();
        if (first != null) {
            PosConfig.Page page = config.getPage(first.getPageNumber());
            res.initialPage = toPageDto(page);
        } else {
            res.initialPage = null;
        }

        return res;
    }

    private static ImportResponse.PageDto toPageDto(PosConfig.Page page) {
        ImportResponse.PageDto dto = new ImportResponse.PageDto();
        dto.pageNumber = page.getPageNumber();
        dto.cols = page.getCols();
        dto.rows = page.getRows();
        dto.buttons = page.getButtons().stream().map(b -> {
            ImportResponse.ButtonDto bd = new ImportResponse.ButtonDto();
            bd.col = b.getCol();
            bd.row = b.getRow();
            bd.label = b.getLabel();
            bd.styleKey = b.getStyleKey();
            bd.itemCode = b.getItemCode();
            return bd;
        }).collect(Collectors.toList());
        return dto;
    }
}
