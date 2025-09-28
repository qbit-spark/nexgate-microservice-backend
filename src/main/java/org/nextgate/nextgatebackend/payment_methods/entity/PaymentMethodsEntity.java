package org.nextgate.nextgatebackend.payment_methods.entity;

import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;
import org.nextgate.nextgatebackend.payment_methods.utils.PaymentMethodDetailsJsonConverter;
import org.nextgate.nextgatebackend.payment_methods.utils.BillingAddressJsonConverter;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment_methods", indexes = {
        @Index(name = "idx_owner_id", columnList = "owner_id"),
        @Index(name = "idx_payment_method_type", columnList = "paymentMethodType"),
        @Index(name = "idx_is_default", columnList = "isDefault"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_owner_default", columnList = "owner_id, isDefault"),
        @Index(name = "idx_owner_active", columnList = "owner_id, isActive")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID paymentMethodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private AccountEntity owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodsType paymentMethodType;

    // Store the complete payment method details as JSON
    @Convert(converter = PaymentMethodDetailsJsonConverter.class)
    @Column(name = "method_details", columnDefinition = "jsonb")
    private PaymentMethodDetails methodDetails;

    // Store billing address as JSON
    @Convert(converter = BillingAddressJsonConverter.class)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private BillingAddress billingAddress;

    // Store metadata as JSON
    @Convert(converter = MetadataJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isVerified = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Nested classes for JSON mapping
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDetails {
        private String billingAddressId;

        // Credit/Debit Card fields
        private String cardType;
        private String cardNumber; // tokenized
        private String expiry;
        private String cardholderName;

        // PayPal fields
        private String email;
        private String paypalId;

        // Bank Transfer fields
        private String bankName;
        private String accountNumber; // tokenized
        private String routingNumber;
        private String accountType;
        private String accountHolderName;

        // Cryptocurrency fields
        private String cryptoType;
        private String walletAddress;
        private String network;

        // Mobile Payment fields
        private String provider;
        private String deviceId;

        // Wallet fields
        private String walletType;
        private String walletId;

        // Gift Card fields
        private String pin; // tokenized
        private Double balance;
        private String currency;

        // Cash on Delivery fields
        private String instructions;

        // MNO Billing fields
        private String phoneNumber;
        private String mccMnc;

        // Common fields
        private String token;
        private Map<String, Object> gatewayMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingAddress {
        private String billingAddressId;
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.isDefault == null) {
            this.isDefault = false;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isVerified == null) {
            this.isVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}