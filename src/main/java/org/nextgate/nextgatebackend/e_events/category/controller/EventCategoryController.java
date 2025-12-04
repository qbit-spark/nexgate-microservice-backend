package org.nextgate.nextgatebackend.e_events.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.category.payloads.CreateEventCategoryRequest;
import org.nextgate.nextgatebackend.e_events.category.payloads.EventCategoryResponse;
import org.nextgate.nextgatebackend.e_events.category.payloads.UpdateEventCategoryRequest;
import org.nextgate.nextgatebackend.e_events.category.service.EventsCategoryService;
import org.nextgate.nextgatebackend.e_events.category.utils.mappers.EventCategoryMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/e-events/categories")
@RequiredArgsConstructor
public class EventCategoryController {

    private final EventsCategoryService categoryService;
    private final EventCategoryMapper categoryMapper;

    // 1. Create Category
    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createCategory(
            @Valid @RequestBody CreateEventCategoryRequest request)
            throws ItemNotFoundException, AccessDeniedException, ItemReadyExistException {

        EventsCategoryEntity created = categoryService.createCategory(request);
        EventCategoryResponse response = categoryMapper.toResponse(created);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Category created successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    // 2. Update Category
    @PutMapping("/{categoryId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateEventCategoryRequest request)
            throws ItemNotFoundException, AccessDeniedException, ItemReadyExistException {

        EventsCategoryEntity updated = categoryService.updateCategory(categoryId, request);
        EventCategoryResponse response = categoryMapper.toResponse(updated);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Category updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    // 3. Get Category by ID
    @GetMapping("/{categoryId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getCategoryById(
            @PathVariable UUID categoryId)
            throws ItemNotFoundException {

        EventsCategoryEntity category = categoryService.getCategoryById(categoryId);
        EventCategoryResponse response = categoryMapper.toResponse(category);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Category retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    // 4. Get Category by Slug
    @GetMapping("/slug/{slug}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getCategoryBySlug(
            @PathVariable String slug)
            throws ItemNotFoundException {

        EventsCategoryEntity category = categoryService.getCategoryBySlug(slug);
        EventCategoryResponse response = categoryMapper.toResponse(category);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Category retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    // 5. Get All Categories
    @GetMapping("/all")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllCategories() {

        List<EventsCategoryEntity> categories = categoryService.getAllCategories();
        List<EventCategoryResponse> responses = categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Categories retrieved successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);
    }

    // 6. Get Paginated Categories
    @GetMapping("/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPaginatedCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<EventsCategoryEntity> categoryPage = categoryService.getPaginatedCategories(page, size);

        Page<EventCategoryResponse> responsePage = categoryPage.map(categoryMapper::toResponse);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Categories retrieved successfully",
                responsePage
        );

        return ResponseEntity.ok(successResponse);
    }

    // 7. Seed Categories
    @PostMapping("/seed")
    public ResponseEntity<GlobeSuccessResponseBuilder> seedCategories()
            throws ItemNotFoundException, AccessDeniedException {

        List<EventsCategoryEntity> seededCategories = categoryService.seedCategories();
        List<EventCategoryResponse> responses = seededCategories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Categories seeded successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);
    }
}