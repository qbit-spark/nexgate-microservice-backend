package org.nextgate.nextgatebackend.financial_system.transaction_history.enums;

public enum TransactionType {
    WALLET_TOPUP,
    WALLET_WITHDRAWAL,
    PURCHASE,
    PURCHASE_REFUND,
    SALE,
    SALE_REFUND,
    PLATFORM_FEE_COLLECTED,
    GROUP_PURCHASE,
    GROUP_REFUND,
    INSTALLMENT_PAYMENT,
    INSTALLMENT_REFUND,
    ESCROW_HOLD,
    ESCROW_RELEASE,
    ESCROW_REFUND,

    FREE_PRODUCT_ORDER,
    FREE_EVENT_BOOKING_ORDER
}