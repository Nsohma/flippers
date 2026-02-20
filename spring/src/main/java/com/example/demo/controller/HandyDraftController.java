package com.example.demo.controller;

import com.example.demo.controller.dto.AddHandyCategoryRequest;
import com.example.demo.controller.dto.AddHandyItemRequest;
import com.example.demo.controller.dto.ItemCatalogResponse;
import com.example.demo.controller.dto.ReorderHandyCategoriesRequest;
import com.example.demo.controller.dto.ReorderHandyItemsRequest;
import com.example.demo.controller.dto.SwapHandyCategoriesRequest;
import com.example.demo.service.AddHandyCategoryUseCase;
import com.example.demo.model.ItemCatalog;
import com.example.demo.service.DeleteHandyCategoryUseCase;
import com.example.demo.service.AddHandyItemUseCase;
import com.example.demo.service.DeleteHandyItemUseCase;
import com.example.demo.service.GetHandyCatalogUseCase;
import com.example.demo.service.ReorderHandyCategoriesUseCase;
import com.example.demo.service.ReorderHandyItemsUseCase;
import com.example.demo.service.SwapHandyCategoriesUseCase;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pos/drafts/{draftId}/handy-categories")
public class HandyDraftController {

    private final GetHandyCatalogUseCase getHandyCatalogUseCase;
    private final AddHandyCategoryUseCase addHandyCategoryUseCase;
    private final DeleteHandyCategoryUseCase deleteHandyCategoryUseCase;
    private final SwapHandyCategoriesUseCase swapHandyCategoriesUseCase;
    private final ReorderHandyCategoriesUseCase reorderHandyCategoriesUseCase;
    private final ReorderHandyItemsUseCase reorderHandyItemsUseCase;
    private final AddHandyItemUseCase addHandyItemUseCase;
    private final DeleteHandyItemUseCase deleteHandyItemUseCase;

    public HandyDraftController(
            GetHandyCatalogUseCase getHandyCatalogUseCase,
            AddHandyCategoryUseCase addHandyCategoryUseCase,
            DeleteHandyCategoryUseCase deleteHandyCategoryUseCase,
            SwapHandyCategoriesUseCase swapHandyCategoriesUseCase,
            ReorderHandyCategoriesUseCase reorderHandyCategoriesUseCase,
            ReorderHandyItemsUseCase reorderHandyItemsUseCase,
            AddHandyItemUseCase addHandyItemUseCase,
            DeleteHandyItemUseCase deleteHandyItemUseCase
    ) {
        this.getHandyCatalogUseCase = getHandyCatalogUseCase;
        this.addHandyCategoryUseCase = addHandyCategoryUseCase;
        this.deleteHandyCategoryUseCase = deleteHandyCategoryUseCase;
        this.swapHandyCategoriesUseCase = swapHandyCategoriesUseCase;
        this.reorderHandyCategoriesUseCase = reorderHandyCategoriesUseCase;
        this.reorderHandyItemsUseCase = reorderHandyItemsUseCase;
        this.addHandyItemUseCase = addHandyItemUseCase;
        this.deleteHandyItemUseCase = deleteHandyItemUseCase;
    }

    @GetMapping
    public ItemCatalogResponse getHandyCategories(@PathVariable String draftId) {
        ItemCatalog catalog = getHandyCatalogUseCase.getHandyCatalog(draftId);
        return ItemCatalogResponse.from(catalog);
    }

    @PostMapping
    public ItemCatalogResponse addHandyCategory(
            @PathVariable String draftId,
            @RequestBody AddHandyCategoryRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        ItemCatalog updatedCatalog = addHandyCategoryUseCase.add(
                draftId,
                req.description
        );
        return ItemCatalogResponse.from(updatedCatalog);
    }

    @DeleteMapping("/{categoryCode}")
    public ItemCatalogResponse deleteHandyCategory(
            @PathVariable String draftId,
            @PathVariable String categoryCode
    ) {
        ItemCatalog updatedCatalog = deleteHandyCategoryUseCase.delete(
                draftId,
                categoryCode
        );
        return ItemCatalogResponse.from(updatedCatalog);
    }

    @PatchMapping("/swap")
    public ItemCatalogResponse swapHandyCategories(
            @PathVariable String draftId,
            @RequestBody SwapHandyCategoriesRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        ItemCatalog updatedCatalog = swapHandyCategoriesUseCase.swap(
                draftId,
                req.fromCategoryCode,
                req.toCategoryCode
        );
        return ItemCatalogResponse.from(updatedCatalog);
    }

    @PatchMapping("/reorder")
    public ItemCatalogResponse reorderHandyCategories(
            @PathVariable String draftId,
            @RequestBody ReorderHandyCategoriesRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (req.fromIndex == null || req.toIndex == null) {
            throw new IllegalArgumentException("fromIndex and toIndex are required");
        }
        ItemCatalog updatedCatalog = reorderHandyCategoriesUseCase.reorder(
                draftId,
                req.fromIndex,
                req.toIndex
        );
        return ItemCatalogResponse.from(updatedCatalog);
    }

    @PatchMapping("/{categoryCode}/items/reorder")
    public ItemCatalogResponse reorderHandyItems(
            @PathVariable String draftId,
            @PathVariable String categoryCode,
            @RequestBody ReorderHandyItemsRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (req.fromIndex == null || req.toIndex == null) {
            throw new IllegalArgumentException("fromIndex and toIndex are required");
        }
        ItemCatalog updatedCatalog = reorderHandyItemsUseCase.reorder(
                draftId,
                categoryCode,
                req.fromIndex,
                req.toIndex
        );
        return ItemCatalogResponse.from(updatedCatalog);
    }

    @DeleteMapping("/{categoryCode}/items/{itemIndex}")
    public ItemCatalogResponse deleteHandyItem(
            @PathVariable String draftId,
            @PathVariable String categoryCode,
            @PathVariable int itemIndex
    ) {
        ItemCatalog updatedCatalog = deleteHandyItemUseCase.delete(
                draftId,
                categoryCode,
                itemIndex
        );
        return ItemCatalogResponse.from(updatedCatalog);
    }

    @PostMapping("/{categoryCode}/items")
    public ItemCatalogResponse addHandyItem(
            @PathVariable String draftId,
            @PathVariable String categoryCode,
            @RequestBody AddHandyItemRequest req
    ) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }
        ItemCatalog updatedCatalog = addHandyItemUseCase.add(
                draftId,
                categoryCode,
                req.sourceCategoryCode,
                req.itemCode
        );
        return ItemCatalogResponse.from(updatedCatalog);
    }
}
