package org.nextgate.nextgatebackend.notification_system.publisher.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for Wallet notification data
 * Prepares data in the format expected by notification templates
 *
 * All methods return Map<String, Object> that can be directly used in NotificationEvent.data
 */
public class WalletNotificationMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== WALLET TOP-UP ====================

    /**
     * Map wallet top-up notification data
     *
     * Use this when customer adds money to their wallet
     *
     * Template variables:
     * - {{customer.name}}
     * - {{customer.email}}
     * - {{transaction.type}} = "CREDIT"
     * - {{transaction.amount}}
     * - {{transaction.id}}
     * - {{wallet.currentBalance}}
     * - {{timestamp}}
     *
     * @param customerName Customer's full name
     * @param customerEmail Customer's email
     * @param topUpAmount Amount being added
     * @param newBalance Current balance after top-up
     * @param transactionId Transaction reference
     * @return Formatted data map for notification
     */
    public static Map<String, Object> mapWalletTopUp(
            String customerName,
            String customerEmail,
            BigDecimal topUpAmount,
            BigDecimal newBalance,
            String transactionId) {

        Map<String, Object> data = new HashMap<>();

        // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", customerName);
        customer.put("email", customerEmail);
        data.put("customer", customer);

        // Transaction information
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", "CREDIT");
        transaction.put("amount", topUpAmount.toString());
        transaction.put("id", transactionId);
        data.put("transaction", transaction);

        // Wallet information
        Map<String, Object> wallet = new HashMap<>();
        wallet.put("currentBalance", newBalance.toString());
        data.put("wallet", wallet);

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    /**
     * Simplified wallet top-up - when email is not needed
     */
    public static Map<String, Object> mapWalletTopUp(
            String customerName,
            BigDecimal topUpAmount,
            BigDecimal newBalance,
            String transactionId) {

        Map<String, Object> data = new HashMap<>();

        // Customer information
        data.put("customer", Map.of("name", customerName));

        // Transaction information
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", "CREDIT");
        transaction.put("amount", topUpAmount.toString());
        transaction.put("id", transactionId);
        data.put("transaction", transaction);

        // Wallet information
        data.put("wallet", Map.of("currentBalance", newBalance.toString()));

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    // ==================== WALLET WITHDRAWAL ====================

    /**
     * Map wallet withdrawal notification data
     *
     * Use this when customer withdraws money from their wallet
     *
     * Template variables:
     * - {{customer.name}}
     * - {{transaction.type}} = "DEBIT"
     * - {{transaction.amount}}
     * - {{transaction.id}}
     * - {{wallet.currentBalance}}
     * - {{timestamp}}
     */
    public static Map<String, Object> mapWalletWithdrawal(
            String customerName,
            BigDecimal withdrawalAmount,
            BigDecimal newBalance,
            String transactionId) {

        Map<String, Object> data = new HashMap<>();

        // Customer information
        data.put("customer", Map.of("name", customerName));

        // Transaction information
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", "DEBIT");
        transaction.put("amount", withdrawalAmount.toString());
        transaction.put("id", transactionId);
        data.put("transaction", transaction);

        // Wallet information
        data.put("wallet", Map.of("currentBalance", newBalance.toString()));

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    // ==================== FULL WALLET UPDATE (with previous balance) ====================

    /**
     * Map complete wallet balance update with before/after comparison
     *
     * Use this when you want to show old balance vs new balance
     *
     * Template variables:
     * - {{customer.name}}
     * - {{customer.email}}
     * - {{transaction.type}}
     * - {{transaction.amount}}
     * - {{transaction.id}}
     * - {{wallet.previousBalance}}
     * - {{wallet.currentBalance}}
     * - {{wallet.change}}
     * - {{timestamp}}
     */
    public static Map<String, Object> mapWalletBalanceUpdate(
            String customerName,
            String customerEmail,
            String transactionType,  // "CREDIT" or "DEBIT"
            BigDecimal transactionAmount,
            String transactionId,
            BigDecimal previousBalance,
            BigDecimal currentBalance) {

        Map<String, Object> data = new HashMap<>();

        // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", customerName);
        customer.put("email", customerEmail);
        data.put("customer", customer);

        // Transaction information
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", transactionType);
        transaction.put("amount", transactionAmount.toString());
        transaction.put("id", transactionId);
        data.put("transaction", transaction);

        // Wallet information with comparison
        Map<String, Object> wallet = new HashMap<>();
        wallet.put("previousBalance", previousBalance.toString());
        wallet.put("currentBalance", currentBalance.toString());

        // Calculate change
        BigDecimal change = currentBalance.subtract(previousBalance);
        wallet.put("change", change.toString());

        data.put("wallet", wallet);

        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return data;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper to create customer info map
     */
    private static Map<String, Object> createCustomerInfo(String name, String email) {
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", name);
        if (email != null && !email.isEmpty()) {
            customer.put("email", email);
        }
        return customer;
    }

    /**
     * Helper to create transaction info map
     */
    private static Map<String, Object> createTransactionInfo(
            String type,
            BigDecimal amount,
            String transactionId) {

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("type", type);
        transaction.put("amount", amount.toString());
        transaction.put("id", transactionId);
        return transaction;
    }
}