package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.utils.StringListJsonConverter;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopType;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.VerificationBadge;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "shops", indexes = {
        @Index(name = "idx_shop_owner", columnList = "owner_id"),
        @Index(name = "idx_shop_category", columnList = "category_id"),
        @Index(name = "idx_shop_status", columnList = "status"),
        @Index(name = "idx_shop_location", columnList = "latitude, longitude"),
        @Index(name = "idx_shop_slug", columnList = "shopSlug")
})
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID shopId;

    // Basic Info
    @Column(nullable = false, length = 100)
    private String shopName;

    @Column(unique = true, nullable = false, length = 120)
    private String shopSlug; // URL-friendly name (auto-generated from shopName)

    @Column(length = 500)
    private String shopDescription;

    @Column(length = 50)
    private String tagline; // Short catchy phrase

    // Shop Images
    @Column(name = "shop_images", columnDefinition = "jsonb")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> shopImages = new ArrayList<>();

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    // Ownership & Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AccountEntity owner;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "categoryId", insertable = false, updatable = false)
    private ShopCategoryEntity category;

    // Shop Type & Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShopType shopType = ShopType.PHYSICAL; // PHYSICAL, ONLINE, HYBRID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShopStatus status = ShopStatus.PENDING; // PENDING, ACTIVE, SUSPENDED, CLOSED

    // Contact Information
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "website_url", columnDefinition = "TEXT")
    private String websiteUrl;

    @Column(name = "social_media_links", columnDefinition = "jsonb")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> socialMediaLinks = new ArrayList<>();

    // Location Information (for physical/hybrid shops)
    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String region;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 3)
    private String countryCode = "TZ"; // Default to Tanzania

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "location_notes", columnDefinition = "TEXT")
    private String locationNotes; // Additional directions/landmarks

    // Business Information
    @Column(name = "registration_number", length = 50)
    private String businessRegistrationNumber;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "established_year")
    private Integer establishedYear;

    // Verification & Trust
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_badge")
    private VerificationBadge verificationBadge; // BRONZE, SILVER, GOLD, PREMIUM

    @Column(name = "trust_score", precision = 3, scale = 2)
    private BigDecimal trustScore = BigDecimal.ZERO;

    // Activity Tracking
    @Column(name = "last_seen_time")
    private LocalDateTime lastSeenTime;

    // Featured & Promotion
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "featured_until")
    private LocalDateTime featuredUntil;

    @Column(name = "promotion_text", columnDefinition = "TEXT")
    private String promotionText;

    // System Fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "id", insertable = false, updatable = false)
    private AccountEntity approvedByUser;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by", referencedColumnName = "id", insertable = false, updatable = false)
    private AccountEntity deletedByUser;

    // Utility Methods
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (shopSlug == null && shopName != null) {
            shopSlug = generateSlugFromName(shopName);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateSlugFromName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // Business Logic Methods
    public boolean isPhysicalShop() {
        return shopType == ShopType.PHYSICAL || shopType == ShopType.HYBRID;
    }

    public boolean isOnlineShop() {
        return shopType == ShopType.ONLINE || shopType == ShopType.HYBRID;
    }
}