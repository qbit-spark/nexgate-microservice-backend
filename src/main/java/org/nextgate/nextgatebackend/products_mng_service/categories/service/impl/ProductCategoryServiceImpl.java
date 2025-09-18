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
import org.nextgate.nextgatebackend.products_mng_service.categories.payload.ProductCategoryResponse;
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
    @Transactional
    public GlobeSuccessResponseBuilder createProductCategory(CreateProductCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException {

        // Check if category name already exists
        if (productCategoryRepo.existsByCategoryName(request.getCategoryName())) {
            throw new ItemReadyExistException("Product category with this name already exists");
        }

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

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
        category.setCreatedBy(user.getId());
        category.setEditedBy(user.getId());

        // Save category
        ProductCategoryEntity savedCategory = productCategoryRepo.save(category);
        ProductCategoryResponse response = buildCategoryResponse(savedCategory);

        log.info("Product category created successfully: {} by user: {}",
                savedCategory.getCategoryName(), user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product category created successfully",
                response
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder updateProductCategory(UUID categoryId, CreateProductCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException {

        // Find existing category
        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        // Check if new name already exists (if name is being changed)
        if (!category.getCategoryName().equals(request.getCategoryName()) &&
                productCategoryRepo.existsByCategoryName(request.getCategoryName())) {
            throw new ItemReadyExistException("Product category with this name already exists");
        }

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Update category fields
        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setCategoryIconUrl(request.getCategoryIconUrl());

        // Handle parent category update
        if (request.getParentCategoryId() != null) {
            ProductCategoryEntity parentCategory = productCategoryRepo.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException("Parent category not found"));
            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null); // Remove parent if not provided
        }

        category.setEditedBy(user.getId());

        // Save updated category
        ProductCategoryEntity updatedCategory = productCategoryRepo.save(category);
        ProductCategoryResponse response = buildCategoryResponse(updatedCategory);

        log.info("Product category updated successfully: {} by user: {}",
                updatedCategory.getCategoryName(), user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product category updated successfully",
                response
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductCategory(UUID categoryId) throws ItemNotFoundException {

        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        ProductCategoryResponse response = buildCategoryResponse(category);

        return GlobeSuccessResponseBuilder.success(
                "Product category retrieved successfully",
                response
        );
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

        List<ProductCategoryResponse> responses = categories.stream()
                .map(this::buildCategoryResponse)
                .toList();

        return GlobeSuccessResponseBuilder.success(
                "Product categories retrieved successfully",
                responses
        );
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

        List<ProductCategoryResponse> responses = parentCategories.stream()
                .map(this::buildCategoryResponse)
                .toList();

        return GlobeSuccessResponseBuilder.success(
                "Parent categories retrieved successfully",
                responses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getChildCategories(UUID parentId) throws ItemNotFoundException {

        ProductCategoryEntity parentCategory = productCategoryRepo.findById(parentId)
                .orElseThrow(() -> new ItemNotFoundException("Parent category not found"));

        List<ProductCategoryEntity> childCategories =
                productCategoryRepo.findByParentCategoryAndIsActiveTrueOrderByCreatedTimeDesc(parentCategory);

        List<ProductCategoryResponse> responses = childCategories.stream()
                .map(this::buildCategoryResponse)
                .toList();

        return GlobeSuccessResponseBuilder.success(
                "Child categories retrieved successfully",
                responses
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder deleteProductCategory(UUID categoryId) throws ItemNotFoundException {

        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        // Check if already inactive
        if (!category.getIsActive()) {
            throw new ItemNotFoundException("Product category is already inactive");
        }

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Soft delete - mark as inactive
        category.setIsActive(false);
        category.setEditedBy(user.getId());
        productCategoryRepo.save(category);

        log.info("Product category deleted successfully: {} by user: {}",
                category.getCategoryName(), user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product category deleted successfully"
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder activateProductCategory(UUID categoryId) throws ItemNotFoundException {

        ProductCategoryEntity category = productCategoryRepo.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        // Check if already active
        if (category.getIsActive()) {
            throw new ItemNotFoundException("Product category is already active");
        }

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Activate category
        category.setIsActive(true);
        category.setEditedBy(user.getId());
        productCategoryRepo.save(category);

        log.info("Product category activated successfully: {} by user: {}",
                category.getCategoryName(), user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product category activated successfully"
        );
    }

    // HELPER METHODS

    private ProductCategoryResponse buildCategoryResponse(ProductCategoryEntity category) {
        ProductCategoryResponse.ProductCategoryResponseBuilder builder = ProductCategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryDescription(category.getCategoryDescription())
                .categoryIconUrl(category.getCategoryIconUrl())
                .createdTime(category.getCreatedTime())
                .editedTime(category.getEditedTime())
                .isActive(category.getIsActive())
                .createdBy(category.getCreatedBy())
                .editedBy(category.getEditedBy());

        // Safely handle parent category information
        if (category.getParentCategory() != null) {
            builder.parentCategoryId(category.getParentCategory().getCategoryId())
                    .parentCategoryName(category.getParentCategory().getCategoryName());
        }

        return builder.build();
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