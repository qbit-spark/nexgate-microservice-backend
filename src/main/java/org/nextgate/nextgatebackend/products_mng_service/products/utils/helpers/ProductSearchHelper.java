// Create new ProductSearchHelper.java

package org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductSearchResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductSummaryResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.SearchContext;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchHelper {

    private final ShopRepo shopRepo;
    private final AccountRepo accountRepo;
    private final ProductRepo productRepo;
    private final ProductBuildResponseHelper productBuildResponseHelper;

    // Main search orchestration method with enhanced LIKE patterns
    public Page<ProductEntity> executeProductSearch(ShopEntity shop, String query, List<ProductStatus> statuses, Pageable pageable) {
        // Build enhanced search pattern
        String searchPattern = buildEnhancedSearchPattern(query);

        return productRepo.searchProductsWithLike(shop, searchPattern, statuses, pageable);
    }

    // Build enhanced search pattern for LIKE queries
    public String buildEnhancedSearchPattern(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "%";
        }

        String cleaned = query.trim().toLowerCase();

        // Handle multi-word queries
        if (cleaned.contains(" ")) {
            // For "dell precision", search for "%dell%precision%"
            String[] words = cleaned.split("\\s+");
            StringBuilder pattern = new StringBuilder("%");

            for (String word : words) {
                if (!word.isEmpty()) {
                    pattern.append(word).append("%");
                }
            }

            return pattern.toString();
        }

        // Single word: "dell" becomes "%dell%"
        return "%" + cleaned + "%";
    }

    // Add method to build multiple patterns for OR search (future enhancement)
    public List<String> buildMultipleSearchPatterns(String query) {
        List<String> patterns = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return patterns;
        }

        String cleaned = query.trim().toLowerCase();

        // Add main pattern
        patterns.add("%" + cleaned + "%");

        // If multi-word, add individual word patterns
        if (cleaned.contains(" ")) {
            String[] words = cleaned.split("\\s+");
            for (String word : words) {
                if (!word.isEmpty() && word.length() > 1) {
                    patterns.add("%" + word + "%");
                }
            }
        }

        return patterns;
    }

    // Determine search context (user permissions)
    public SearchContext determineSearchContext(UUID shopId) throws ItemNotFoundException {
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        SearchContext.SearchContextBuilder contextBuilder = SearchContext.builder()
                .shop(shop)
                .isPublicUser(true)
                .isShopOwner(false)
                .isSystemAdmin(false)
                .canSearchAllStatuses(false);

        try {
            // Try to get authenticated user
            AccountEntity user = getAuthenticatedAccount();
            contextBuilder.user(user)
                    .isPublicUser(false);

            // Check if user is system admin
            boolean isSystemAdmin = validateSystemRoles(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), user);
            contextBuilder.isSystemAdmin(isSystemAdmin);

            // Check if user is shop owner
            boolean isShopOwner = shop.getOwner().getId().equals(user.getId());
            contextBuilder.isShopOwner(isShopOwner);

            // Determine if user can search all statuses
            boolean canSearchAllStatuses = isSystemAdmin || isShopOwner;
            contextBuilder.canSearchAllStatuses(canSearchAllStatuses);

        } catch (ItemNotFoundException e) {
            // User not authenticated - remain as public user
            // For public users, we need to validate shop is public accessible
            if (shop.getIsDeleted() || !shop.isApproved() || shop.getStatus() != ShopStatus.ACTIVE) {
                throw new ItemNotFoundException("Shop not available");
            }
        }

        return contextBuilder.build();
    }

    // Validate and sanitize search query
    public String validateAndSanitizeQuery(String query) throws ItemNotFoundException {
        if (query == null || query.trim().isEmpty()) {
            throw new ItemNotFoundException("Search query is required");
        }

        String sanitized = query.trim();

        // Minimum length check
        if (sanitized.length() < 2) {
            throw new ItemNotFoundException("Search query must be at least 2 characters");
        }

        // Maximum length check
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        // Remove potentially harmful characters for SQL injection prevention
        sanitized = sanitized.replaceAll("[<>\"'%;()&+]", "");

        return sanitized;
    }

    // Determine which product statuses to search based on user permissions
    public List<ProductStatus> determineSearchStatuses(List<ProductStatus> requestedStatuses, SearchContext context) {
        if (context.isPublicUser()) {
            // Public users can only search ACTIVE products
            return List.of(ProductStatus.ACTIVE);
        }

        if (!context.isCanSearchAllStatuses()) {
            // Authenticated but not owner/admin - still only ACTIVE
            return List.of(ProductStatus.ACTIVE);
        }

        // Owner/Admin can specify statuses or search all
        if (requestedStatuses != null && !requestedStatuses.isEmpty()) {
            return requestedStatuses;
        }

        // Default for owners/admins - search all statuses except archived
        return List.of(ProductStatus.ACTIVE, ProductStatus.DRAFT, ProductStatus.INACTIVE, ProductStatus.OUT_OF_STOCK);
    }

    // Build sort criteria
    public Sort buildSearchSort(String sortBy, Sort.Direction direction, String query) {
        if ("relevance".equals(sortBy)) {
            // For relevance, we'd ideally use database full-text search scoring
            // For now, fallback to created date desc (most recent first)
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return Sort.by(direction, sortBy);
    }

    // Build search response
    public GlobeSuccessResponseBuilder buildSearchResponse(Page<ProductEntity> searchResults, String query,
                                                           SearchContext context, List<ProductStatus> searchedStatuses) {

        // Build product responses
        List<ProductSummaryResponse> productResponses = searchResults.getContent().stream()
                .map(product -> {
                    ProductSummaryResponse response = productBuildResponseHelper.buildProductSummaryResponse(product);

                    // Add search-specific metadata for owners/admins
                    if (!context.isPublicUser()) {
                        // Could add status badges, edit links, etc.
                    }

                    return response;
                })
                .toList();

        // Build shop summary
        ProductSummaryResponse.ShopSummaryForProducts shopSummary = productBuildResponseHelper.buildShopSummaryForProducts(context.getShop());

        // Build search metadata
        ProductSearchResponse.SearchMetadata searchMetadata = ProductSearchResponse.SearchMetadata.builder()
                .searchQuery(query)
                .searchedStatuses(context.isPublicUser() ? null : searchedStatuses)
                .userType(context.isPublicUser() ? "PUBLIC" :
                        context.isShopOwner() ? "SHOP_OWNER" :
                                context.isSystemAdmin() ? "ADMIN" : "AUTHENTICATED")
                .canSearchAllStatuses(context.isCanSearchAllStatuses())
                .build();

        // Build contents object
        ProductSearchResponse.SearchContents contentsData = ProductSearchResponse.SearchContents.builder()
                .shop(shopSummary)
                .products(productResponses)
                .totalProducts(productResponses.size())
                .searchMetadata(searchMetadata)
                .build();

        // Build final response
        ProductSearchResponse finalResponse = ProductSearchResponse.builder()
                .contents(contentsData)
                .currentPage(searchResults.getNumber() + 1)
                .pageSize(searchResults.getSize())
                .totalElements(searchResults.getTotalElements())
                .totalPages(searchResults.getTotalPages())
                .hasNext(searchResults.hasNext())
                .hasPrevious(searchResults.hasPrevious())
                .build();

        String resultMessage = String.format("Found %d products matching '%s'",
                searchResults.getTotalElements(), query);

        return GlobeSuccessResponseBuilder.success(resultMessage, finalResponse);
    }

    // Validate sort fields
    public String validateSortField(String sortBy) {
        List<String> allowedSortFields = List.of(
                "relevance", "createdAt", "updatedAt", "productName", "price", "stockQuantity", "brand"
        );

        return allowedSortFields.contains(sortBy) ? sortBy : "relevance";
    }

    // PRIVATE HELPER METHODS

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String userName;

            // Handle different types of principals
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                userName = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                userName = (String) principal;
            } else {
                throw new ItemNotFoundException("Invalid authentication principal type");
            }

            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    private boolean validateSystemRoles(List<String> requiredRoles, AccountEntity account) {
        if (account == null || account.getRoles() == null) {
            return false;
        }

        Set<String> accountRoleNames = account.getRoles().stream()
                .map(role -> role.getRoleName())
                .collect(Collectors.toSet());

        return requiredRoles.stream().anyMatch(accountRoleNames::contains);
    }
}