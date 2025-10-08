package org.nextgate.nextgatebackend.installment_purchase.enums;

import lombok.Getter;

@Getter
public enum PaymentFrequency {
    DAILY(1, "Daily", "day"),
    WEEKLY(7, "Weekly", "week"),
    BI_WEEKLY(14, "Bi-weekly", "2 weeks"),
    SEMI_MONTHLY(-1, "Semi-monthly", "twice a month"),
    MONTHLY(30, "Monthly", "month"),
    QUARTERLY(90, "Quarterly", "quarter"),
    CUSTOM_DAYS(0, "Custom", "custom period");

    private final int baseDays;
    private final String displayName;
    private final String unitName;

    PaymentFrequency(int baseDays, String displayName, String unitName) {
        this.baseDays = baseDays;
        this.displayName = displayName;
        this.unitName = unitName;
    }

    public boolean isCustom() {
        return this == CUSTOM_DAYS;
    }

    public boolean isSpecial() {
        return this == SEMI_MONTHLY;
    }
}