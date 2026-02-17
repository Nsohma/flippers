package com.example.demo.controller;

import com.example.demo.controller.dto.AddButtonRequest;
import com.example.demo.controller.dto.DeleteButtonRequest;
import com.example.demo.controller.dto.ItemCatalogResponse;
import com.example.demo.controller.dto.PageResponse;
import com.example.demo.controller.dto.SwapButtonsRequest;
import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.service.ExportPosUseCase;
import com.example.demo.service.GetPageUseCase;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pos/drafts")
public class PosDraftController {

    private final GetPageUseCase getPageUseCase;
    private final ExportPosUseCase exportPosUseCase;

    public PosDraftController(GetPageUseCase getPageUseCase, ExportPosUseCase exportPosUseCase) {
        this.getPageUseCase = getPageUseCase;
        this.exportPosUseCase = exportPosUseCase;
    }

    @GetMapping("/{draftId}/pages/{pageNumber}")
    public PageResponse getPage(@PathVariable String draftId, @PathVariable int pageNumber) {
        PosConfig.Page page = getPageUseCase.getPage(draftId, pageNumber);
        return toPageResponse(page);
    }

    @PatchMapping("/{draftId}/pages/{pageNumber}/buttons/swap")
    public PageResponse swapButtons(
            @PathVariable String draftId,
            @PathVariable int pageNumber,
            @RequestBody SwapButtonsRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PosConfig.Page page = getPageUseCase.swapButtons(
                draftId,
                pageNumber,
                req.fromCol,
                req.fromRow,
                req.toCol,
                req.toRow
        );
        return toPageResponse(page);
    }

    @GetMapping("/{draftId}/item-categories")
    public ItemCatalogResponse getItemCategories(@PathVariable String draftId) {
        ItemCatalog catalog = getPageUseCase.getItemCatalog(draftId);
        return toItemCatalogResponse(catalog);
    }

    @PostMapping("/{draftId}/pages/{pageNumber}/buttons")
    public PageResponse addButton(
            @PathVariable String draftId,
            @PathVariable int pageNumber,
            @RequestBody AddButtonRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PosConfig.Page page = getPageUseCase.addButton(
                draftId,
                pageNumber,
                req.col,
                req.row,
                req.categoryCode,
                req.itemCode
        );
        return toPageResponse(page);
    }

    @DeleteMapping("/{draftId}/pages/{pageNumber}/buttons")
    public PageResponse deleteButton(
            @PathVariable String draftId,
            @PathVariable int pageNumber,
            @RequestBody DeleteButtonRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PosConfig.Page page = getPageUseCase.deleteButton(
                draftId,
                pageNumber,
                req.buttonId
        );
        return toPageResponse(page);
    }

    @GetMapping(
            value = "/{draftId}/export",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]> exportDraft(@PathVariable String draftId) {
        byte[] exported = exportPosUseCase.exportExcel(draftId);
        String filename = draftId + "_edited.xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString()
                )
                .body(exported);
    }

    private static PageResponse toPageResponse(PosConfig.Page page) {
        PageResponse res = new PageResponse();
        res.pageNumber = page.getPageNumber();
        res.cols = page.getCols();
        res.rows = page.getRows();
        res.buttons = page.getButtons().stream().map(b -> {
            PageResponse.ButtonDto d = new PageResponse.ButtonDto();
            d.col = b.getCol();
            d.row = b.getRow();
            d.label = b.getLabel();
            d.styleKey = b.getStyleKey();
            d.itemCode = b.getItemCode();
            d.unitPrice = b.getUnitPrice();
            d.buttonId = b.getButtonId();
            return d;
        }).collect(Collectors.toList());
        return res;
    }

    private static ItemCatalogResponse toItemCatalogResponse(ItemCatalog catalog) {
        ItemCatalogResponse response = new ItemCatalogResponse();
        response.categories = catalog.getCategories().stream().map(category -> {
            ItemCatalogResponse.CategoryDto dto = new ItemCatalogResponse.CategoryDto();
            dto.code = category.getCode();
            dto.description = category.getDescription();
            dto.items = category.getItems().stream().map(item -> {
                ItemCatalogResponse.ItemDto itemDto = new ItemCatalogResponse.ItemDto();
                itemDto.itemCode = item.getItemCode();
                itemDto.itemName = item.getItemName();
                itemDto.unitPrice = item.getUnitPrice();
                return itemDto;
            }).collect(Collectors.toList());
            return dto;
        }).collect(Collectors.toList());
        return response;
    }
}
