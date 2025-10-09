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
    private UUID itemId;

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

    @Column(length = 500)
    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal itemTotal;

    @Enumerated(EnumType.STRING)
    private FulfillmentTiming fulfillmentTiming;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(columnDefinition = "TEXT")
    private String itemNotes;

    @PrePersist
    @PreUpdate
    protected void calculateItemTotal() {
        if (priceAtPurchase != null && quantity != null) {
            this.itemTotal = priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
            if (discount != null) {
                this.itemTotal = this.itemTotal.subtract(discount);
            }
        }
    }

    public boolean isInstallmentItem() {
        return installmentAgreementId != null;
    }
}