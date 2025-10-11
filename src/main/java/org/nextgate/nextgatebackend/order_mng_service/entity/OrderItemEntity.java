package org.nextgate.nextgatebackend.order_mng_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_order", columnList = "order_id"),
        @Index(name = "idx_order_item_product", columnList = "product_id"),
        @Index(name = "idx_order_item_installment", columnList = "installmentAgreementId")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_item_id")  // ← Add explicit column name
    private UUID orderItemId;  // ← CHANGED from itemId to orderItemId

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "orderId", nullable = false)
    @JsonIgnoreProperties({"items", "hibernateLazyInitializer", "handler"})
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "productId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductEntity product;

    @Column(name = "installment_agreement_id")
    private UUID installmentAgreementId;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(length = 100)
    private String productSlug;  // ← ADD THIS if missing

    @Column(length = 500)
    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;  // ← RENAME from priceAtPurchase

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;  // ← ADD THIS

    @Column(precision = 10, scale = 2)
    private BigDecimal tax;  // ← ADD THIS

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;  // ← RENAME from itemTotal

    @Enumerated(EnumType.STRING)
    private FulfillmentTiming fulfillmentTiming;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(columnDefinition = "TEXT")
    private String itemNotes;

    @PrePersist
    @PreUpdate
    protected void calculateItemTotal() {
        if (unitPrice != null && quantity != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            if (discount != null) {
                this.subtotal = this.subtotal.subtract(discount);
            }

            // Calculate total (subtotal + tax)
            this.total = this.subtotal;
            if (tax != null) {
                this.total = this.total.add(tax);
            }
        }
    }

    public boolean isInstallmentItem() {
        return installmentAgreementId != null;
    }
}