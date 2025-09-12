package org.nextgate.nextgatebackend.shops_mng_service.categories.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.categories.paylaod.CreateShopCategoryRequest;
import org.nextgate.nextgatebackend.shops_mng_service.categories.repo.ShopCategoryRepo;
import org.nextgate.nextgatebackend.shops_mng_service.categories.service.ShopCategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShopCategoryServiceImpl implements ShopCategoryService {

    private final ShopCategoryRepo shopCategoryRepo;
    private final AccountRepo accountRepo;

    @Override
    public GlobeSuccessResponseBuilder createShopCategory(CreateShopCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException {

        // Check if a category already exists
        ShopCategoryEntity existingCategory = shopCategoryRepo.findByCategoryNameAndIsDeletedFalse(request.getCategoryName());
        if (existingCategory != null) {
            throw new ItemReadyExistException("Category with this name already exists");
        }

        // Create a new category
        ShopCategoryEntity newCategory = new ShopCategoryEntity();
        newCategory.setCategoryName(request.getCategoryName());
        newCategory.setCategoryDescription(request.getCategoryDescription());
        newCategory.setCreatedTime(LocalDateTime.now());
        newCategory.setEditedTime(LocalDateTime.now());
        newCategory.setIsActive(true);
        newCategory.setIsDeleted(false);

        UUID accountId = getAuthenticatedAccount().getAccountId();
        newCategory.setCreatedBy(accountId);
        newCategory.setEditedBy(accountId);

        ShopCategoryEntity savedCategory = shopCategoryRepo.save(newCategory);
        log.info("Created shop category: {} by user: {}", savedCategory.getCategoryName(), accountId);

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category created successfully")
                .success(true)
                .data(savedCategory)
                .build();
    }

    @Override
    public GlobeSuccessResponseBuilder deleteShopCategory(String categoryName) throws ItemNotFoundException {

        ShopCategoryEntity category = shopCategoryRepo.findByCategoryNameAndIsDeletedFalse(categoryName);
        if (category == null) {
            throw new ItemNotFoundException("Shop category not found");
        }

        // Soft delete
        category.setIsDeleted(true);
        category.setIsActive(false);
        category.setEditedTime(LocalDateTime.now());
        category.setEditedBy(getAuthenticatedAccount().getAccountId());

        shopCategoryRepo.save(category);
        log.info("Deleted shop category: {}", categoryName);

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category deleted successfully")
                .success(true)
                .build();
    }

    @Override
    public GlobeSuccessResponseBuilder updateShopCategory(String categoryName, CreateShopCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException {

        ShopCategoryEntity category = shopCategoryRepo.findByCategoryNameAndIsDeletedFalse(categoryName);
        if (category == null) {
            throw new ItemNotFoundException("Shop category not found");
        }

        // Check if a new name already exists (if different from the current)
        if (!categoryName.equals(request.getCategoryName())) {
            ShopCategoryEntity existingCategory = shopCategoryRepo.findByCategoryNameAndIsDeletedFalse(request.getCategoryName());
            if (existingCategory != null) {
                throw new ItemReadyExistException("Category with this name already exists");
            }
        }

        // Update category
        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setEditedTime(LocalDateTime.now());
        category.setEditedBy(getAuthenticatedAccount().getAccountId());

        ShopCategoryEntity updatedCategory = shopCategoryRepo.save(category);
        log.info("Updated shop category: {}", categoryName);

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category updated successfully")
                .success(true)
                .data(updatedCategory)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getShopCategory(UUID shopCategoryId) throws ItemNotFoundException {

        Optional<ShopCategoryEntity> categoryOpt = shopCategoryRepo.findById(shopCategoryId);
        if (categoryOpt.isEmpty() || categoryOpt.get().getIsDeleted()) {
            throw new ItemNotFoundException("Shop category not found");
        }

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category fetched successfully")
                .success(true)
                .data(categoryOpt.get())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getShopCategoryByName(String categoryName) throws ItemNotFoundException {

        ShopCategoryEntity category = shopCategoryRepo.findByCategoryNameAndIsDeletedFalse(categoryName);
        if (category == null) {
            throw new ItemNotFoundException("Shop category not found");
        }

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category fetched successfully")
                .success(true)
                .data(category)
                .build();
    }

    // NON-PAGEABLE LIST METHODS

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllShopCategories() {

        List<ShopCategoryEntity> categories = shopCategoryRepo.findByIsDeletedFalse();

        return GlobeSuccessResponseBuilder.builder()
                .message("All shop categories fetched successfully")
                .success(true)
                .data(categories)
                .build();
    }

    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllActiveShopCategories() {

        List<ShopCategoryEntity> categories = shopCategoryRepo.findByIsDeletedFalseAndIsActiveTrue();

        return GlobeSuccessResponseBuilder.builder()
                .message("All active shop categories fetched successfully")
                .success(true)
                .data(categories)
                .build();
    }

    // PAGEABLE LIST METHODS

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllShopCategoriesPaged(int page, int size) {

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Page<ShopCategoryEntity> categoryPage = shopCategoryRepo.findByIsDeletedFalseOrderByCreatedTimeDesc(pageable);

        return buildPagedResponse(categoryPage, "Shop categories fetched successfully");
    }

    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllActiveShopCategoriesPaged(int page, int size) {

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Page<ShopCategoryEntity> categoryPage = shopCategoryRepo.findByIsDeletedFalseAndIsActiveTrueOrderByCategoryNameAsc(pageable);

        return buildPagedResponse(categoryPage, "Active shop categories fetched successfully");
    }

    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllShopCategoriesOrderedByName(int page, int size) {

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Page<ShopCategoryEntity> categoryPage = shopCategoryRepo.findByIsDeletedFalseOrderByCategoryNameAsc(pageable);

        return buildPagedResponse(categoryPage, "Shop categories fetched successfully (ordered by name)");
    }

    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder searchShopCategoriesByName(String searchTerm, int page, int size) {

        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Page<ShopCategoryEntity> categoryPage = shopCategoryRepo
                .findByIsDeletedFalseAndCategoryNameContainingIgnoreCase(searchTerm, pageable);

        return buildPagedResponse(categoryPage, "Shop categories search completed successfully");
    }

    // UTILITY METHODS

    private GlobeSuccessResponseBuilder buildPagedResponse(Page<ShopCategoryEntity> categoryPage, String message) {

        var responseData = new Object() {
            public final List<ShopCategoryEntity> categories = categoryPage.getContent();
            public final int currentPage = categoryPage.getNumber();
            public final int pageSize = categoryPage.getSize();
            public final long totalElements = categoryPage.getTotalElements();
            public final int totalPages = categoryPage.getTotalPages();
            public final boolean hasNext = categoryPage.hasNext();
            public final boolean hasPrevious = categoryPage.hasPrevious();
            public final boolean isFirst = categoryPage.isFirst();
            public final boolean isLast = categoryPage.isLast();
        };

        log.info("Fetched {} categories for page {}", categoryPage.getContent().size(), categoryPage.getNumber());

        return GlobeSuccessResponseBuilder.builder()
                .message(message)
                .success(true)
                .data(responseData)
                .build();
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}