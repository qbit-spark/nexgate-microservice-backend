package org.nextgate.nextgatebackend.notification_system.publisher.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for Payment notification data
 * Prepares data in the format expected by notification templates
 */
public class PaymentNotificationMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== PAYMENT SUCCESS ====================

    /**
     * Map payment received/success notification
     *
     * Use this when payment is successfully processed
     *
     * Template variables:
     * - {{customer.name}}
     * - {{customer.email}}
     * - {{payment.amount}}
     * - {{payment.method}}
     * - {{payment.status}}
     * - {{payment.currency}}
     * - {{escrow.id}}
     * - {{escrow.number}}
     * - {{checkoutSessionId}}
     * - {{timestamp}}
     *
     * @param customerName Customer's name
     * @param customerEmail Customer's email
     * @param amount Payment amount
     * @param currency Currency (e.g., "TZS")
     * @param paymentMethod Payment method (e.g., "WALLET")
     * @param escrowId Escrow ID
     * @param escrowNumber Escrow number
     * @param checkoutSessionId Checkout session ID
     * @return Formatted data map for notification
     */
    public static Map<String, Object> mapPaymentReceived(
            String customerName,
            String customerEmail,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String escrowId,
            String escrowNumber,
            String checkoutSessionId) {

        Map<String, Object> data = new HashMap<>();

        // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", customerName);
        customer.put("email", customerEmail);
        data.put("customer", customer);

        // Payment information
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", amount.toString());
        payment.put("method", paymentMethod);
        payment.put("status", "SUCCESS");
        payment.put("currency", currency);
        data.put("payment", payment);

        // Escrow information
        Map<String, Object> escrow = new HashMap<>();
        escrow.put("id", escrowId);
        escrow.put("number", escrowNumber);
        data.put("escrow", escrow);

        // Checkout session
        data.put("checkoutSessionId", checkoutSessionId);

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    /**
     * Simplified payment success - minimal info
     */
    public static Map<String, Object> mapPaymentReceived(
            String customerName,
            BigDecimal amount,
            String paymentMethod,
            String escrowNumber) {

        Map<String, Object> data = new HashMap<>();

        // Customer
        data.put("customer", Map.of("name", customerName));

        // Payment
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", amount.toString());
        payment.put("method", paymentMethod);
        payment.put("status", "SUCCESS");
        data.put("payment", payment);

        // Escrow
        data.put("escrow", Map.of("number", escrowNumber));

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    // ==================== PAYMENT FAILURE ====================

    /**
     * Map payment failure notification
     *
     * Template variables:
     * - {{customer.name}}
     * - {{customer.email}}
     * - {{payment.amount}}
     * - {{payment.method}}
     * - {{payment.failureReason}}
     * - {{checkoutSessionId}}
     * - {{timestamp}}
     */
    public static Map<String, Object> mapPaymentFailure(
            String customerName,
            String customerEmail,
            BigDecimal amount,
            String paymentMethod,
            String failureReason,
            String checkoutSessionId) {

        Map<String, Object> data = new HashMap<>();

        // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", customerName);
        customer.put("email", customerEmail);
        data.put("customer", customer);

        // Payment information
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", amount.toString());
        payment.put("method", paymentMethod);
        payment.put("status", "FAILED");
        payment.put("failureReason", failureReason);
        data.put("payment", payment);

        // Checkout session
        data.put("checkoutSessionId", checkoutSessionId);

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    /**
     * Simplified payment failure
     */
    public static Map<String, Object> mapPaymentFailure(
            String customerName,
            BigDecimal amount,
            String failureReason) {

        Map<String, Object> data = new HashMap<>();

        // Customer
        data.put("customer", Map.of("name", customerName));

        // Payment
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", amount.toString());
        payment.put("status", "FAILED");
        payment.put("failureReason", failureReason);
        data.put("payment", payment);

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }
}