package com.example.demo.controller;

import com.example.demo.controller.dto.PageResponse;
import com.example.demo.controller.dto.SwapButtonsRequest;
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
}
