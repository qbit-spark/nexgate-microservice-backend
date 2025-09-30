package org.nextgate.nextgatebackend.group_purchase_mng.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "group_purchase_instances", indexes = {
        @Index(name = "idx_group_product", columnList = "product_id"),
        @Index(name = "idx_group_shop", columnList = "shop_id"),
        @Index(name = "idx_group_status", columnList = "status"),
        @Index(name = "idx_group_expires", columnList = "expiresAt"),
        @Index(name = "idx_group_code", columnList = "groupCode"),
        @Index(name = "idx_group_created", columnList = "createdAt")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GroupPurchaseInstanceEntity {

    // ========================================
    // PRIMARY IDENTIFICATION
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID groupInstanceId;

    @Column(unique = true, nullable = false, length = 20)
    private String groupCode;

    // ========================================
    // RELATIONSHIPS
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "productId", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", referencedColumnName = "shopId", nullable = false)
    private ShopEntity shop;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", referencedColumnName = "id", nullable = false)
    private AccountEntity initiator;

    @OneToMany(mappedBy = "groupInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GroupParticipantEntity> participants = new ArrayList<>();

    // ========================================
    // PRODUCT SNAPSHOT (Immutable - copied at creation)
    // ========================================

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(length = 500)
    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal regularPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal groupPrice;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column
    private Integer maxPerCustomer;

    @Column(nullable = false)
    private Integer durationHours;

    // ========================================
    // CURRENT STATE
    // ========================================

    @Column(nullable = false)
    private Integer seatsOccupied = 0;

    @Column(nullable = false)
    private Integer totalParticipants = 0;

    // ========================================
    // STATUS
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.OPEN;

    // ========================================
    // TIMING
    // ========================================

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime updatedAt;

    // ========================================
    // SOFT DELETE
    // ========================================

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column
    private LocalDateTime deletedAt;

    @Column
    private UUID deletedBy;

    @Column(length = 500)
    private String deleteReason;

    // ========================================
    // SYSTEM FIELDS
    // ========================================

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = MetadataJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    @Column
    private Integer priority = 0;

    // ========================================
    // LIFECYCLE HOOKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (groupCode == null) {
            groupCode = generateGroupCode();
        }
        if (expiresAt == null && durationHours != null) {
            expiresAt = createdAt.plusHours(durationHours);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

    public Integer getSeatsRemaining() {
        return totalSeats - seatsOccupied;
    }

    public boolean isFull() {
        return seatsOccupied >= totalSeats;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == GroupStatus.OPEN && !isExpired() && !isDeleted;
    }

    public boolean canAcceptSeats(Integer quantity) {
        return getSeatsRemaining() >= quantity;
    }

    public boolean userCanBuyQuantity(Integer quantity) {
        if (maxPerCustomer == null || maxPerCustomer == 0) {
            return true;
        }
        return quantity <= maxPerCustomer;
    }

    public void addSeats(Integer quantity) {
        this.seatsOccupied += quantity;
        this.totalParticipants++;
    }

    public void removeSeats(Integer quantity) {
        this.seatsOccupied -= quantity;
        this.totalParticipants = Math.max(0, totalParticipants - 1);
    }

    public BigDecimal calculateSavings() {
        return regularPrice.subtract(groupPrice);
    }

    public BigDecimal calculateSavingsPercentage() {
        if (regularPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return calculateSavings()
                .multiply(BigDecimal.valueOf(100))
                .divide(regularPrice, 2, BigDecimal.ROUND_HALF_UP);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private String generateGroupCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder("GP-");
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}