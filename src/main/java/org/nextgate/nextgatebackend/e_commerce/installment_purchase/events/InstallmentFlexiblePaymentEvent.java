
package org.nextgate.nextgatebackend.e_commerce.installment_purchase.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPaymentEntity;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a customer makes a flexible payment
 * (paying more or less than the scheduled amount).
 *
 * Triggers:
 * - Payment confirmation with schedule impact
 * - Updated payment schedule notification
 * - Interest savings calculation
 * - Flexible payment appreciation message
 */
@Getter
public class InstallmentFlexiblePaymentEvent extends ApplicationEvent {

    private final UUID paymentId;
    private final InstallmentPaymentEntity payment;
    private final LocalDateTime paymentAt;
    private final BigDecimal scheduledAmount;
    private final BigDecimal actualAmount;
    private final BigDecimal extraAmount;
    private final BigDecimal interestImpact;
    private final boolean scheduleChanged;
    private final List<Map<String, Object>> updatedSchedule;

    public InstallmentFlexiblePaymentEvent(
            Object source,
            UUID paymentId,
            InstallmentPaymentEntity payment,
            LocalDateTime paymentAt,
            BigDecimal scheduledAmount,
            BigDecimal actualAmount,
            BigDecimal extraAmount,
            BigDecimal interestImpact,
            boolean scheduleChanged,
            List<Map<String, Object>> updatedSchedule
    ) {
        super(source);
        this.paymentId = paymentId;
        this.payment = payment;
        this.paymentAt = paymentAt;
        this.scheduledAmount = scheduledAmount;
        this.actualAmount = actualAmount;
        this.extraAmount = extraAmount;
        this.interestImpact = interestImpact;
        this.scheduleChanged = scheduleChanged;
        this.updatedSchedule = updatedSchedule;
    }
}