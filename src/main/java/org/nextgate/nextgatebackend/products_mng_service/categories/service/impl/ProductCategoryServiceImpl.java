package org.nextgate.nextgatebackend.products_mng_service.categories.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.categories.entity.ProductCategoryEntity;
import org.nextgate.nextgatebackend.products_mng_service.categories.payload.CreateProductCategoryRequest;
import org.nextgate.nextgatebackend.products_mng_service.categories.repo.ProductCategoryRepo;
import org.nextgate.nextgatebackend.products_mng_service.categories.service.ProductCategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepo productCategoryRepo;
    private final AccountRepo accountRepo;

    @Override
    public GlobeSuccessResponseBuilder createProductCategory(CreateProductCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException {

        // Check if category already exists
        if (productCategoryRepo.existsByCategoryName(request.getCategoryName())) {
            throw new ItemReadyExistException("Product category with this name already exists");
        }

        // Create new category
        ProductCategoryEntity category = new ProductCategoryEntity();
        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setCategoryIconUrl(request.getCategoryIconUrl());

        // Handle parent category if provided
        if (request.getParentCategoryId() != null) {
            ProductCategoryEntity parentCategory = productCategoryRepo.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException("Parent category not found"));
            category.setParentCategory(parentCategory);
        }

        category.setIsActive(true);
        UUID accountId = getAuthenticatedAccount().getAccountId();
        category.setCreatedBy(accountId);
        category.setEditedBy(accountId);

        ProductCategoryEntity savedCategory = productCategoryRepo.save(category);

        return GlobeSuccessResponseBuilder.builder()
                .message("Product category created successfully")
                .success(true)
                .data(savedCategory)
                .build();
    }

    @Override
    public GlobeSuccessResponseBuilder updateProductCategory(UUID categoryId, CreateProductCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException {

        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        // Check if new name already exists (if different from current)
        if (!category.getCategoryName().equals(request.getCategoryName()) &&
                productCategoryRepo.existsByCategoryName(request.getCategoryName())) {
            throw new ItemReadyExistException("Product category with this name already exists");
        }

        // Update category
        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setCategoryIconUrl(request.getCategoryIconUrl());

        // Handle parent category
        if (request.getParentCategoryId() != null) {
            ProductCategoryEntity parentCategory = productCategoryRepo.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException("Parent category not found"));
            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null);
        }

        category.setEditedBy(getAuthenticatedAccount().getAccountId());

        ProductCategoryEntity updatedCategory = productCategoryRepo.save(category);

        return GlobeSuccessResponseBuilder.builder()
                .message("Product category updated successfully")
                .success(true)
                .data(updatedCategory)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductCategory(UUID categoryId) throws ItemNotFoundException {
        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        return GlobeSuccessResponseBuilder.builder()
                .message("Product category retrieved successfully")
                .success(true)
                .data(category)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllProductCategories(Boolean isActive) {
        List<ProductCategoryEntity> categories;

        if (isActive != null) {
            categories = productCategoryRepo.findByIsActiveOrderByCreatedTimeDesc(isActive);
        } else {
            categories = productCategoryRepo.findAll(Sort.by(Sort.Direction.DESC, "createdTime"));
        }

        return GlobeSuccessResponseBuilder.builder()
                .message("Product categories retrieved successfully")
                .success(true)
                .data(categories)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCategoryEntity> getAllProductCategoriesPaged(Boolean isActive, int page, int size) {
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdTime"));

        if (isActive != null) {
            return productCategoryRepo.findByIsActiveOrderByCreatedTimeDesc(isActive, pageable);
        } else {
            return productCategoryRepo.findAll(pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getParentCategories() {
        List<ProductCategoryEntity> parentCategories =
                productCategoryRepo.findByParentCategoryIsNullAndIsActiveTrueOrderByCreatedTimeDesc();

        return GlobeSuccessResponseBuilder.builder()
                .message("Parent categories retrieved successfully")
                .success(true)
                .data(parentCategories)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getChildCategories(UUID parentId) throws ItemNotFoundException {
        ProductCategoryEntity parentCategory = productCategoryRepo.findById(parentId)
                .orElseThrow(() -> new ItemNotFoundException("Parent category not found"));

        List<ProductCategoryEntity> childCategories =
                productCategoryRepo.findByParentCategoryAndIsActiveTrueOrderByCreatedTimeDesc(parentCategory);

        return GlobeSuccessResponseBuilder.builder()
                .message("Child categories retrieved successfully")
                .success(true)
                .data(childCategories)
                .build();
    }

    @Override
    public GlobeSuccessResponseBuilder deleteProductCategory(UUID categoryId) throws ItemNotFoundException {
        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        // Check if already inactive
        if (!category.getIsActive()) {
            throw new ItemNotFoundException("Product category is already inactive");
        }

        category.setIsActive(false);
        category.setEditedBy(getAuthenticatedAccount().getAccountId());
        productCategoryRepo.save(category);

        return GlobeSuccessResponseBuilder.builder()
                .message("Product category deleted successfully")
                .success(true)
                .data("Product category deleted successfully")
                .build();
    }

    @Override
    public GlobeSuccessResponseBuilder activateProductCategory(UUID categoryId) throws ItemNotFoundException {
        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        // Check if already active
        if (category.getIsActive()) {
            throw new ItemNotFoundException("Product category is already active");
        }

        category.setIsActive(true);
        category.setEditedBy(getAuthenticatedAccount().getAccountId());
        productCategoryRepo.save(category);

        return GlobeSuccessResponseBuilder.builder()
                .message("Product category activated successfully")
                .success(true)
                .data("Product category activated successfully")
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