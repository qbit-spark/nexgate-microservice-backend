package org.nextgate.nextgatebackend.e_events.category.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.entity.Roles;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.category.payloads.CreateEventCategoryRequest;
import org.nextgate.nextgatebackend.e_events.category.payloads.UpdateEventCategoryRequest;
import org.nextgate.nextgatebackend.e_events.category.repo.EventsCategoryRepository;
import org.nextgate.nextgatebackend.e_events.category.service.EventsCategoryService;
import org.nextgate.nextgatebackend.e_events.category.utils.CategorySeeder;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCategoryServiceImpl implements EventsCategoryService {

    private final AccountRepo accountRepo;
    private final EventsCategoryRepository categoryRepository;

    @Override
    @Transactional
    public EventsCategoryEntity createCategory(CreateEventCategoryRequest createEventCategoryRequest) throws AccessDeniedException, ItemNotFoundException, ItemReadyExistException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Step 2: Validate user has permission
        validateRole(currentUser, "ROLE_STAFF_ADMIN", "ROLE_SUPER_ADMIN");

        // Step 3: Check duplicate by name (case-insensitive)
        String categoryName = createEventCategoryRequest.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(categoryName)) {
            throw new ItemReadyExistException(
                    "Category with name '" + categoryName + "' already exists"
            );
        }

        // Step 4: Generate slug from name
        String generatedSlug = generateSlug(categoryName);

        // Step 5: Handle slug collision (add number suffix if needed)
        if (categoryRepository.existsBySlug(generatedSlug)) {
            int counter = 1;
            String newSlug = generatedSlug;
            while (categoryRepository.existsBySlug(newSlug)) {
                newSlug = generatedSlug + "-" + counter;
                counter++;
            }
            generatedSlug = newSlug;
            log.info("Slug collision handled. Using slug: {}", generatedSlug);
        }

        // Step 6: Build the entity
        EventsCategoryEntity category = EventsCategoryEntity.builder()
                .name(categoryName)
                .slug(generatedSlug)
                .description(createEventCategoryRequest.getDescription() != null
                        ? createEventCategoryRequest.getDescription().trim()
                        : null)
                .iconUrl(createEventCategoryRequest.getIconUrl())
                .colorCode(createEventCategoryRequest.getColorCode())
                .isActive(createEventCategoryRequest.getIsActive())
                .isFeatured(createEventCategoryRequest.getIsFeatured())
                .eventCount(0L)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        // Step 7: Save to a database

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public EventsCategoryEntity updateCategory(UUID categoryId, UpdateEventCategoryRequest updateRequest) throws ItemNotFoundException, AccessDeniedException, ItemReadyExistException {

        // Step 1: Get an authenticated user
        AccountEntity currentUser = getAuthenticatedAccount();

        // Step 2: Validate user has permission
        validateRole(currentUser, "ROLE_STAFF_ADMIN", "ROLE_SUPER_ADMIN");

        // Step 3: Find the existing category
        EventsCategoryEntity existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Category not found with ID: " + categoryId));

        // Step 4: Handle name update (if provided and changed)
        if (updateRequest.getName() != null && !updateRequest.getName().trim().isEmpty()) {
            String newName = updateRequest.getName().trim();

            // Only check duplicate if name is actually changing
            if (!existingCategory.getName().equalsIgnoreCase(newName)) {
                // Check if new name already exists (excluding current category)
                if (categoryRepository.existsByNameIgnoreCaseAndCategoryIdNot(newName, categoryId)) {
                    throw new ItemReadyExistException(
                            "Category with name '" + newName + "' already exists"
                    );
                }

                // Update name
                existingCategory.setName(newName);

                // Regenerate slug since name changed
                String newSlug = generateSlug(newName);

                // Handle slug collision (excluding current category)
                if (categoryRepository.existsBySlugAndCategoryIdNot(newSlug, categoryId)) {
                    int counter = 1;
                    String tempSlug = newSlug;
                    while (categoryRepository.existsBySlugAndCategoryIdNot(tempSlug, categoryId)) {
                        tempSlug = newSlug + "-" + counter;
                        counter++;
                    }
                    newSlug = tempSlug;
                }

                existingCategory.setSlug(newSlug);
            }
        }

        // Step 5: Update other fields (only if provided)
        if (updateRequest.getDescription() != null) {
            existingCategory.setDescription(updateRequest.getDescription().trim());
        }

        if (updateRequest.getIconUrl() != null) {
            existingCategory.setIconUrl(updateRequest.getIconUrl());
        }

        if (updateRequest.getColorCode() != null) {
            existingCategory.setColorCode(updateRequest.getColorCode());
        }

        if (updateRequest.getIsActive() != null) {
            existingCategory.setIsActive(updateRequest.getIsActive());
        }

        if (updateRequest.getIsFeatured() != null) {
            existingCategory.setIsFeatured(updateRequest.getIsFeatured());
        }

        // Step 6: Set who updated
        existingCategory.setUpdatedBy(currentUser);

        // Step 7: Save (updatedAt will be set automatically by @LastModifiedDate)
        return categoryRepository.save(existingCategory);
    }

    @Override
    public EventsCategoryEntity getCategoryById(UUID categoryId) throws ItemNotFoundException {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ItemNotFoundException("Category not found with ID: " + categoryId));
    }

    @Override
    public EventsCategoryEntity getCategoryBySlug(String slug) throws ItemNotFoundException {

        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ItemNotFoundException("Category not found with slug: " + slug));
    }

    @Override
    public List<EventsCategoryEntity> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Page<EventsCategoryEntity> getPaginatedCategories(int page, int size) {

        int zeroBasedPage = page - 1;

        Pageable pageable = PageRequest.of(zeroBasedPage, size);

        return categoryRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public List<EventsCategoryEntity> seedCategories() throws ItemNotFoundException, AccessDeniedException {
        AccountEntity currentUser = getAuthenticatedAccount();
        validateRole(currentUser, "ROLE_STAFF_ADMIN", "ROLE_SUPER_ADMIN");

        List<EventsCategoryEntity> defaultCategories = CategorySeeder.getDefaultCategories();
        List<EventsCategoryEntity> categoriesToSave = new ArrayList<>();

        for (EventsCategoryEntity category : defaultCategories) {
            if (!categoryRepository.existsByNameIgnoreCase(category.getName())) {
                category.setCreatedBy(currentUser);
                categoriesToSave.add(category);
            }
        }

        if (categoriesToSave.isEmpty()) {
            return categoryRepository.findAll();
        }

        // Save only the new categories
        return categoryRepository.saveAll(categoriesToSave);
    }


    /**
     * Generate URL-friendly slug from name
     */
    private String generateSlug(String name) {
        return name
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special chars
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Remove duplicate hyphens
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
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

    public void validateRole(AccountEntity account, String... requiredRoles) throws AccessDeniedException {
        if (account == null) {
            throw new AccessDeniedException("Account not found");
        }

        if (account.getRoles() == null || account.getRoles().isEmpty()) {
            throw new AccessDeniedException("Account has no roles assigned");
        }

        // Get account's role names
        Set<String> accountRoleNames = account.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());

        // Check if account has any of the required roles
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(accountRoleNames::contains);

        if (!hasRequiredRole) {
            throw new AccessDeniedException("Access denied. Insufficient permissions.");
        }
    }
}