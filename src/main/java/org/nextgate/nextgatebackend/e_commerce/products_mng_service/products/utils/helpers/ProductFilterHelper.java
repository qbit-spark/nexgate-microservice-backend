package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.utils.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductCondition;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload.ProductFilterCriteria;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload.ProductFilterResponse;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload.ProductSummaryResponse;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload.SearchContext;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductFilterHelper {

    private final ProductRepo productRepo;
    private final ProductBuildResponseHelper productBuildResponseHelper;

    // Execute product filtering with criteria
    public Page<ProductEntity> executeProductFilter(ShopEntity shop, ProductFilterCriteria criteria,
                                                    List<ProductStatus> statuses, Pageable pageable) {

        // Use Specification pattern for dynamic filtering
        Specification<ProductEntity> spec = buildFilterSpecification(shop, criteria, statuses);

        return productRepo.findAll(spec, pageable);
    }

    // Build dynamic filter specification - Updated for Spring Data JPA 3.5+
    private Specification<ProductEntity> buildFilterSpecification(ShopEntity shop, ProductFilterCriteria criteria,
                                                                  List<ProductStatus> statuses) {

        Specification<ProductEntity> spec = shopAndNotDeletedSpec(shop, statuses);

        // Chain specifications properly
        if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
            spec = spec.and(priceRangeSpec(criteria.getMinPrice(), criteria.getMaxPrice()));
        }

        if (criteria.getBrands() != null && !criteria.getBrands().isEmpty()) {
            spec = spec.and(brandSpec(criteria.getBrands()));
        }

        if (criteria.getCondition() != null) {
            spec = spec.and(conditionSpec(criteria.getCondition()));
        }

        if (criteria.getCategoryId() != null) {
            spec = spec.and(categorySpec(criteria.getCategoryId()));
        }

        if (criteria.getInStock() != null) {
            spec = spec.and(stockSpec(criteria.getInStock()));
        }

        if (criteria.getOnSale() != null) {
            spec = spec.and(saleSpec(criteria.getOnSale()));
        }

        if (criteria.getIsFeatured() != null) {
            spec = spec.and(featuredSpec(criteria.getIsFeatured()));
        }

        if (criteria.getHasGroupBuying() != null) {
            spec = spec.and(groupBuyingSpec(criteria.getHasGroupBuying()));
        }

        if (criteria.getHasInstallments() != null) {
            spec = spec.and(installmentSpec(criteria.getHasInstallments()));
        }

        return spec;
    }

    // Base specification - shop, not deleted, correct status
    private Specification<ProductEntity> shopAndNotDeletedSpec(ShopEntity shop, List<ProductStatus> statuses) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("shop"), shop),
                cb.equal(root.get("isDeleted"), false),
                root.get("status").in(statuses)
        );
    }

    // Price range specification
    private Specification<ProductEntity> priceRangeSpec(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return cb.conjunction(); // No filter
            }

            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("price"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            } else {
                return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
            }
        };
    }

    // Brand specification
    private Specification<ProductEntity> brandSpec(List<String> brands) {
        return (root, query, cb) -> {
            if (brands == null || brands.isEmpty()) {
                return cb.conjunction();
            }

            // Case insensitive brand matching
            List<String> lowerBrands = brands.stream()
                    .map(String::toLowerCase)
                    .toList();

            return cb.lower(root.get("brand")).in(lowerBrands);
        };
    }

    // Condition specification
    private Specification<ProductEntity> conditionSpec(ProductCondition condition) {
        return (root, query, cb) -> {
            if (condition == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("condition"), condition);
        };
    }

    // Category specification
    private Specification<ProductEntity> categorySpec(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category").get("categoryId"), categoryId);
        };
    }

    // Stock specification
    private Specification<ProductEntity> stockSpec(Boolean inStock) {
        return (root, query, cb) -> {
            if (inStock == null) {
                return cb.conjunction();
            }

            if (inStock) {
                return cb.greaterThan(root.get("stockQuantity"), 0);
            } else {
                return cb.equal(root.get("stockQuantity"), 0);
            }
        };
    }

    // Sale specification
    private Specification<ProductEntity> saleSpec(Boolean onSale) {
        return (root, query, cb) -> {
            if (onSale == null) {
                return cb.conjunction();
            }

            if (onSale) {
                return cb.and(
                        cb.isNotNull(root.get("comparePrice")),
                        cb.greaterThan(root.get("comparePrice"), root.get("price"))
                );
            } else {
                return cb.or(
                        cb.isNull(root.get("comparePrice")),
                        cb.lessThanOrEqualTo(root.get("comparePrice"), root.get("price"))
                );
            }
        };
    }

    // Featured specification
    private Specification<ProductEntity> featuredSpec(Boolean isFeatured) {
        return (root, query, cb) -> {
            if (isFeatured == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isFeatured"), isFeatured);
        };
    }

    // Group buying specification
    private Specification<ProductEntity> groupBuyingSpec(Boolean hasGroupBuying) {
        return (root, query, cb) -> {
            if (hasGroupBuying == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("groupBuyingEnabled"), hasGroupBuying);
        };
    }

    // Installment specification
    private Specification<ProductEntity> installmentSpec(Boolean hasInstallments) {
        return (root, query, cb) -> {
            if (hasInstallments == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("installmentEnabled"), hasInstallments);
        };
    }

    // Build filter response
    public GlobeSuccessResponseBuilder buildFilterResponse(Page<ProductEntity> filterResults,
                                                           ProductFilterCriteria criteria,
                                                           SearchContext context,
                                                           List<ProductStatus> searchedStatuses) {

        // Build product responses
        List<ProductSummaryResponse> productResponses = filterResults.getContent().stream()
                .map(productBuildResponseHelper::buildProductSummaryResponse)
                .toList();

        // Build shop summary
        ProductSummaryResponse.ShopSummaryForProducts shopSummary =
                productBuildResponseHelper.buildShopSummaryForProducts(context.getShop());

        // Build filter metadata
        ProductFilterResponse.FilterMetadata filterMetadata = ProductFilterResponse.FilterMetadata.builder()
                .appliedFilters(criteria)
                .userType(context.isPublicUser() ? "PUBLIC" :
                        context.isShopOwner() ? "SHOP_OWNER" :
                                context.isSystemAdmin() ? "ADMIN" : "AUTHENTICATED")
                .searchedStatuses(context.isPublicUser() ? null : searchedStatuses)
                .hasActiveFilters(criteria.hasAnyFilter())
                .build();

        // Build contents
        ProductFilterResponse.FilterContents contents = ProductFilterResponse.FilterContents.builder()
                .shop(shopSummary)
                .products(productResponses)
                .totalProducts(productResponses.size())
                .filterMetadata(filterMetadata)
                .build();

        // Build final response
        ProductFilterResponse finalResponse = ProductFilterResponse.builder()
                .contents(contents)
                .currentPage(filterResults.getNumber() + 1)
                .pageSize(filterResults.getSize())
                .totalElements(filterResults.getTotalElements())
                .totalPages(filterResults.getTotalPages())
                .hasNext(filterResults.hasNext())
                .hasPrevious(filterResults.hasPrevious())
                .build();

        String message = String.format("Found %d products matching your filters", filterResults.getTotalElements());

        return GlobeSuccessResponseBuilder.success(message, finalResponse);
    }
}