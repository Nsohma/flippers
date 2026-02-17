package com.example.demo.controller;

import com.example.demo.controller.dto.PageResponse;
import com.example.demo.model.PosConfig;
import com.example.demo.service.GetPageUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pos/drafts")
public class PosDraftController {

    private final GetPageUseCase getPageUseCase;

    public PosDraftController(GetPageUseCase getPageUseCase) {
        this.getPageUseCase = getPageUseCase;
    }

    @GetMapping("/{draftId}/pages/{pageNumber}")
    public PageResponse getPage(@PathVariable String draftId, @PathVariable int pageNumber) {
        PosConfig.Page page = getPageUseCase.getPage(draftId, pageNumber);

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
            return d;
        }).collect(Collectors.toList());
        return res;
    }
}
