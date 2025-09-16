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
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        if (shopCategoryRepo.existsByCategoryName(request.getCategoryName())) {
            throw new ItemReadyExistException("Category with this name already exists");
        }

        // Create a new category
        ShopCategoryEntity newCategory = new ShopCategoryEntity();
        newCategory.setCategoryName(request.getCategoryName());
        newCategory.setCategoryDescription(request.getCategoryDescription());
        newCategory.setCategoryIconUrl(request.getCategoryIconUrl());
        newCategory.setCreatedTime(LocalDateTime.now());
        newCategory.setEditedTime(LocalDateTime.now());
        newCategory.setIsActive(true);


        UUID accountId = getAuthenticatedAccount().getAccountId();
        newCategory.setCreatedBy(accountId);
        newCategory.setEditedBy(accountId);

        ShopCategoryEntity savedCategory = shopCategoryRepo.save(newCategory);

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category created successfully")
                .success(true)
                .data(savedCategory)
                .build();
    }


    @Override
    public GlobeSuccessResponseBuilder updateShopCategory(UUID categoryId, CreateShopCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException {

        ShopCategoryEntity category = shopCategoryRepo.findById(categoryId).orElseThrow(
                () -> new ItemNotFoundException("Shop category not found")
        );

        // Check if a new name already exists (if different from the current)
        if (!category.getCategoryName().equals(request.getCategoryName()) && shopCategoryRepo.existsByCategoryName(request.getCategoryName())) {
            throw new ItemReadyExistException("Category with this name already exists");
        }


        // Update category
        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setEditedTime(LocalDateTime.now());
        category.setEditedBy(getAuthenticatedAccount().getAccountId());

        ShopCategoryEntity updatedCategory = shopCategoryRepo.save(category);

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category updated successfully")
                .success(true)
                .data(updatedCategory)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getShopCategory(UUID shopCategoryId) throws ItemNotFoundException {

        ShopCategoryEntity category = shopCategoryRepo.findById(shopCategoryId).orElseThrow(
                () -> new ItemNotFoundException("Shop category not found")
        );

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category fetched successfully")
                .success(true)
                .data(category)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllShopCategories(Boolean isActive) {

        List<ShopCategoryEntity> categories;

        if (isActive != null) {
            categories = shopCategoryRepo.findByIsActive(isActive);
        } else {
            categories = shopCategoryRepo.findAll();
        }

        return GlobeSuccessResponseBuilder.builder()
                .message("All shop categories fetched successfully")
                .success(true)
                .data(categories)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ShopCategoryEntity> getAllShopCategoriesPaged(Boolean isActive, int page, int size) {
        // Convert 1-based page to 0-based and add validation
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdTime"));

        if (isActive != null) {
            return shopCategoryRepo.findByIsActive(isActive, pageable);
        } else {
            return shopCategoryRepo.findAll(pageable);
        }
    }

    @Override
    public GlobeSuccessResponseBuilder deleteShopCategory(UUID shopCategoryId) throws ItemNotFoundException {
        ShopCategoryEntity category = shopCategoryRepo.findById(shopCategoryId).orElseThrow(
                () -> new ItemNotFoundException("Shop category not found")
        );

        //Check it is ready inactive
        if (!category.getIsActive()) {
            throw new ItemNotFoundException("Shop category is already inactive");
        }

        category.setIsActive(false);
        category.setEditedTime(LocalDateTime.now());
        category.setEditedBy(getAuthenticatedAccount().getAccountId());
        shopCategoryRepo.save(category);

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category deleted successfully")
                .success(true)
                .data("Shop category deleted successfully")
                .build();
    }

    @Override
    public GlobeSuccessResponseBuilder activateShopCategory(UUID shopCategoryId) throws ItemNotFoundException {

        ShopCategoryEntity category = shopCategoryRepo.findById(shopCategoryId).orElseThrow(
                () -> new ItemNotFoundException("Shop category not found")
        );

        //Check it is ready inactive
        if (category.getIsActive()) {
            throw new ItemNotFoundException("Shop category is already active, cannot activate again");
        }

        category.setIsActive(true);
        category.setEditedTime(LocalDateTime.now());
        category.setEditedBy(getAuthenticatedAccount().getAccountId());
        shopCategoryRepo.save(category);

        return GlobeSuccessResponseBuilder.builder()
                .message("Shop category activated successfully")
                .success(true)
                .data("Shop category activated successfully")
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