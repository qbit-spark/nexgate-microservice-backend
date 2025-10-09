package org.nextgate.nextgatebackend.order_mng_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.order_mng_service.enums.DeliveryStatus;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderSource;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderStatus;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_buyer", columnList = "buyer_id"),
        @Index(name = "idx_order_seller", columnList = "seller_id"),
        @Index(name = "idx_order_status", columnList = "orderStatus"),
        @Index(name = "idx_order_number", columnList = "orderNumber"),
        @Index(name = "idx_order_source", columnList = "orderSource")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID orderId;

    @Column(unique = true, nullable = false, length = 50)
    private String orderNumber;

    @Column(length = 20)
    private String confirmationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AccountEntity buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "shopId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShopEntity seller;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSource orderSource;

    @Column(name = "checkout_session_id")
    private UUID checkoutSessionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 50)
    private String paymentMethod;

    @Column(precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(precision = 10, scale = 2)
    private BigDecimal amountRemaining;

    @Column(name = "escrow_id")
    private UUID escrowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String deliveryInstructions;

    @Column(nullable = false)
    private Boolean isDeliveryConfirmed = false;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(nullable = false)
    private Boolean isEscrowReleased = false;

    @Column(name = "escrow_released_at")
    private LocalDateTime escrowReleasedAt;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String orderNotes;

    @Column(columnDefinition = "TEXT")
    private String sellerNotes;

    @Column(columnDefinition = "TEXT")
    private String buyerNotes;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(nullable = false, length = 10)
    private String currency = "TZS";

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = MetadataJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }
        if (orderStatus == null) {
            orderStatus = OrderStatus.PENDING_SHIPMENT;
        }
        if (deliveryStatus == null) {
            deliveryStatus = DeliveryStatus.PENDING;
        }
    }

    private String generateOrderNumber() {
        int year = LocalDateTime.now().getYear();
        String randomPart = String.format("%05d", new Random().nextInt(100000));
        return String.format("ORD-%d-%s", year, randomPart);
    }

    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItemEntity item) {
        items.remove(item);
        item.setOrder(null);
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItemEntity::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = subtotal
                .add(shippingFee != null ? shippingFee : BigDecimal.ZERO)
                .add(tax != null ? tax : BigDecimal.ZERO)
                .subtract(discount != null ? discount : BigDecimal.ZERO);
    }

    public boolean canBeCancelled() {
        return orderStatus == OrderStatus.PENDING_SHIPMENT;
    }

    public boolean canBeShipped() {
        return orderStatus == OrderStatus.PENDING_SHIPMENT;
    }

    public boolean canBeConfirmed() {
        return orderStatus == OrderStatus.SHIPPED && !isDeliveryConfirmed;
    }

    public boolean isCompleted() {
        return orderStatus == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return orderStatus == OrderStatus.CANCELLED;
    }

    public int getTotalItemCount() {
        return items.stream()
                .mapToInt(OrderItemEntity::getQuantity)
                .sum();
    }
}