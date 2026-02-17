package com.example.demo.controller;

import com.example.demo.controller.dto.AddButtonRequest;
import com.example.demo.controller.dto.AddCategoryRequest;
import com.example.demo.controller.dto.CategoryStateResponse;
import com.example.demo.controller.dto.DeleteButtonRequest;
import com.example.demo.controller.dto.DraftHistoryResponse;
import com.example.demo.controller.dto.ItemCatalogResponse;
import com.example.demo.controller.dto.PageResponse;
import com.example.demo.controller.dto.SwapButtonsRequest;
import com.example.demo.controller.dto.UpdateCategoryGridRequest;
import com.example.demo.controller.dto.UpdateUnitPriceRequest;
import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import com.example.demo.service.AddButtonUseCase;
import com.example.demo.service.AddCategoryUseCase;
import com.example.demo.service.DeleteButtonUseCase;
import com.example.demo.service.DeleteCategoryUseCase;
import com.example.demo.service.ExportPosUseCase;
import com.example.demo.service.GetDraftHistoryUseCase;
import com.example.demo.service.GetPageUseCase;
import com.example.demo.service.GetItemCatalogUseCase;
import com.example.demo.service.JumpDraftHistoryUseCase;
import com.example.demo.service.RedoDraftUseCase;
import com.example.demo.service.SwapButtonsUseCase;
import com.example.demo.service.UndoDraftUseCase;
import com.example.demo.service.UpdateUnitPriceUseCase;
import com.example.demo.service.UpdateCategoryGridUseCase;
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
    private final GetDraftHistoryUseCase getDraftHistoryUseCase;
    private final JumpDraftHistoryUseCase jumpDraftHistoryUseCase;
    private final SwapButtonsUseCase swapButtonsUseCase;
    private final GetItemCatalogUseCase getItemCatalogUseCase;
    private final AddButtonUseCase addButtonUseCase;
    private final DeleteButtonUseCase deleteButtonUseCase;
    private final UpdateUnitPriceUseCase updateUnitPriceUseCase;
    private final AddCategoryUseCase addCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final UpdateCategoryGridUseCase updateCategoryGridUseCase;
    private final UndoDraftUseCase undoDraftUseCase;
    private final RedoDraftUseCase redoDraftUseCase;
    private final ExportPosUseCase exportPosUseCase;

    public PosDraftController(
            GetPageUseCase getPageUseCase,
            GetDraftHistoryUseCase getDraftHistoryUseCase,
            JumpDraftHistoryUseCase jumpDraftHistoryUseCase,
            SwapButtonsUseCase swapButtonsUseCase,
            GetItemCatalogUseCase getItemCatalogUseCase,
            AddButtonUseCase addButtonUseCase,
            DeleteButtonUseCase deleteButtonUseCase,
            UpdateUnitPriceUseCase updateUnitPriceUseCase,
            AddCategoryUseCase addCategoryUseCase,
            DeleteCategoryUseCase deleteCategoryUseCase,
            UpdateCategoryGridUseCase updateCategoryGridUseCase,
            UndoDraftUseCase undoDraftUseCase,
            RedoDraftUseCase redoDraftUseCase,
            ExportPosUseCase exportPosUseCase
    ) {
        this.getPageUseCase = getPageUseCase;
        this.getDraftHistoryUseCase = getDraftHistoryUseCase;
        this.jumpDraftHistoryUseCase = jumpDraftHistoryUseCase;
        this.swapButtonsUseCase = swapButtonsUseCase;
        this.getItemCatalogUseCase = getItemCatalogUseCase;
        this.addButtonUseCase = addButtonUseCase;
        this.deleteButtonUseCase = deleteButtonUseCase;
        this.updateUnitPriceUseCase = updateUnitPriceUseCase;
        this.addCategoryUseCase = addCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.updateCategoryGridUseCase = updateCategoryGridUseCase;
        this.undoDraftUseCase = undoDraftUseCase;
        this.redoDraftUseCase = redoDraftUseCase;
        this.exportPosUseCase = exportPosUseCase;
    }

    @GetMapping("/{draftId}/pages/{pageNumber}")
    public PageResponse getPage(@PathVariable String draftId, @PathVariable int pageNumber) {
        PosConfig.Page page = getPageUseCase.getPage(draftId, pageNumber);
        return toPageResponse(page);
    }

    @GetMapping("/{draftId}/history")
    public DraftHistoryResponse getHistory(@PathVariable String draftId) {
        PosDraft draft = getDraftHistoryUseCase.getHistory(draftId);
        return toDraftHistoryResponse(draft);
    }

    @PostMapping("/{draftId}/history/jump")
    public CategoryStateResponse jumpHistory(
            @PathVariable String draftId,
            @RequestParam("index") int index,
            @RequestParam(name = "selectedPageNumber", required = false) Integer selectedPageNumber
    ) {
        PosDraft updatedDraft = jumpDraftHistoryUseCase.jumpTo(draftId, index);
        int resolvedSelectedPageNumber = resolveSelectedPageNumber(updatedDraft.getConfig(), selectedPageNumber);
        return toCategoryStateResponse(updatedDraft, resolvedSelectedPageNumber);
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
        PosConfig.Page page = swapButtonsUseCase.swapButtons(
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
        ItemCatalog catalog = getItemCatalogUseCase.getItemCatalog(draftId);
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
        PosConfig.Page page = addButtonUseCase.addButton(
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
        PosConfig.Page page = deleteButtonUseCase.deleteButton(
                draftId,
                pageNumber,
                req.buttonId
        );
        return toPageResponse(page);
    }

    @PatchMapping("/{draftId}/pages/{pageNumber}/buttons/unit-price")
    public PageResponse updateUnitPrice(
            @PathVariable String draftId,
            @PathVariable int pageNumber,
            @RequestBody UpdateUnitPriceRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PosConfig.Page page = updateUnitPriceUseCase.updateUnitPrice(
                draftId,
                pageNumber,
                req.buttonId,
                req.unitPrice
        );
        return toPageResponse(page);
    }

    @PostMapping("/{draftId}/categories")
    public CategoryStateResponse addCategory(
            @PathVariable String draftId,
            @RequestBody AddCategoryRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PosConfig updatedConfig = addCategoryUseCase.addCategory(
                draftId,
                req.name,
                req.cols,
                req.rows,
                req.styleKey
        );
        int selectedPageNumber = updatedConfig.getCategories().stream()
                .mapToInt(PosConfig.Category::getPageNumber)
                .max()
                .orElseThrow();
        return toCategoryStateResponse(updatedConfig, selectedPageNumber);
    }

    @DeleteMapping("/{draftId}/categories/{pageNumber}")
    public CategoryStateResponse deleteCategory(
            @PathVariable String draftId,
            @PathVariable int pageNumber
    ) {
        PosConfig updatedConfig = deleteCategoryUseCase.deleteCategory(draftId, pageNumber);
        PosConfig.Category selected = updatedConfig.firstCategoryOrNull();
        int selectedPageNumber = selected == null ? -1 : selected.getPageNumber();
        return toCategoryStateResponse(updatedConfig, selectedPageNumber);
    }

    @PatchMapping("/{draftId}/categories/{pageNumber}/grid")
    public CategoryStateResponse updateCategoryGrid(
            @PathVariable String draftId,
            @PathVariable int pageNumber,
            @RequestBody UpdateCategoryGridRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PosDraft updatedDraft = updateCategoryGridUseCase.updateCategoryGrid(
                draftId,
                pageNumber,
                req.cols,
                req.rows
        );
        int selectedPageNumber = resolveSelectedPageNumber(updatedDraft.getConfig(), pageNumber);
        return toCategoryStateResponse(updatedDraft, selectedPageNumber);
    }

    @PostMapping("/{draftId}/undo")
    public CategoryStateResponse undo(
            @PathVariable String draftId,
            @RequestParam(name = "selectedPageNumber", required = false) Integer selectedPageNumber
    ) {
        PosDraft updatedDraft = undoDraftUseCase.undo(draftId);
        int resolvedSelectedPageNumber = resolveSelectedPageNumber(updatedDraft.getConfig(), selectedPageNumber);
        return toCategoryStateResponse(updatedDraft, resolvedSelectedPageNumber);
    }

    @PostMapping("/{draftId}/redo")
    public CategoryStateResponse redo(
            @PathVariable String draftId,
            @RequestParam(name = "selectedPageNumber", required = false) Integer selectedPageNumber
    ) {
        PosDraft updatedDraft = redoDraftUseCase.redo(draftId);
        int resolvedSelectedPageNumber = resolveSelectedPageNumber(updatedDraft.getConfig(), selectedPageNumber);
        return toCategoryStateResponse(updatedDraft, resolvedSelectedPageNumber);
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

    private static CategoryStateResponse toCategoryStateResponse(PosDraft draft, int selectedPageNumber) {
        return toCategoryStateResponse(
                draft.getConfig(),
                selectedPageNumber,
                draft.canUndo(),
                draft.canRedo()
        );
    }

    private static CategoryStateResponse toCategoryStateResponse(PosConfig config, int selectedPageNumber) {
        return toCategoryStateResponse(config, selectedPageNumber, null, null);
    }

    private static CategoryStateResponse toCategoryStateResponse(
            PosConfig config,
            int selectedPageNumber,
            Boolean canUndo,
            Boolean canRedo
    ) {
        CategoryStateResponse response = new CategoryStateResponse();
        response.categories = config.getCategories().stream().map(category -> {
            CategoryStateResponse.CategoryDto dto = new CategoryStateResponse.CategoryDto();
            dto.pageNumber = category.getPageNumber();
            dto.cols = category.getCols();
            dto.rows = category.getRows();
            dto.name = category.getName();
            dto.styleKey = category.getStyleKey();
            return dto;
        }).collect(Collectors.toList());
        if (selectedPageNumber > 0) {
            PosConfig.Page page = config.getPage(selectedPageNumber);
            response.page = page == null ? null : toPageResponse(page);
        } else {
            response.page = null;
        }
        response.canUndo = canUndo;
        response.canRedo = canRedo;
        return response;
    }

    private static int resolveSelectedPageNumber(PosConfig config, Integer requestedPageNumber) {
        if (requestedPageNumber != null && requestedPageNumber > 0 && config.getPage(requestedPageNumber) != null) {
            return requestedPageNumber;
        }
        PosConfig.Category first = config.firstCategoryOrNull();
        return first == null ? -1 : first.getPageNumber();
    }

    private static DraftHistoryResponse toDraftHistoryResponse(PosDraft draft) {
        DraftHistoryResponse response = new DraftHistoryResponse();
        response.entries = java.util.stream.IntStream
                .range(0, draft.getHistoryEntries().size())
                .mapToObj(i -> {
                    PosDraft.HistoryEntry entry = draft.getHistoryEntries().get(i);
                    DraftHistoryResponse.EntryDto dto = new DraftHistoryResponse.EntryDto();
                    dto.index = i;
                    dto.action = entry.getAction();
                    dto.timestamp = entry.getTimestamp();
                    return dto;
                })
                .collect(Collectors.toList());
        response.currentIndex = draft.getHistoryIndex();
        response.canUndo = draft.canUndo();
        response.canRedo = draft.canRedo();
        return response;
    }
}
