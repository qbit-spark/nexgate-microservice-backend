package org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo;

import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentPaymentRepo extends JpaRepository<InstallmentPaymentEntity, UUID> {

    // Find all payments for an agreement
    List<InstallmentPaymentEntity> findByAgreementOrderByPaymentNumberAsc(
            InstallmentAgreementEntity agreement);

    // Find payment by number
    Optional<InstallmentPaymentEntity> findByAgreementAndPaymentNumber(
            InstallmentAgreementEntity agreement, Integer paymentNumber);

    // Find payments due today or past
    List<InstallmentPaymentEntity> findByPaymentStatusInAndDueDateLessThanEqual(
            List<PaymentStatus> statuses, LocalDateTime date);

    // Find overdue payments
    List<InstallmentPaymentEntity> findByPaymentStatusAndDueDateBefore(
            PaymentStatus status, LocalDateTime date);

    // Find next scheduled payment for agreement
    Optional<InstallmentPaymentEntity> findFirstByAgreementAndPaymentStatusOrderByPaymentNumberAsc(
            InstallmentAgreementEntity agreement, PaymentStatus status);

    // Count completed payments for agreement
    long countByAgreementAndPaymentStatus(
            InstallmentAgreementEntity agreement, PaymentStatus status);

    List<InstallmentPaymentEntity> findByAgreementAndPaymentStatusOrderByPaymentNumberAsc(
            InstallmentAgreementEntity agreement, PaymentStatus status);

    // NEW: Find all unpaid or partially paid payments in order
    @Query("SELECT p FROM InstallmentPaymentEntity p " +
            "WHERE p.agreement = :agreement " +
            "AND p.paymentStatus IN ('SCHEDULED', 'PENDING', 'PARTIALLY_PAID', 'LATE') " +
            "ORDER BY p.paymentNumber ASC")
    List<InstallmentPaymentEntity> findUnpaidPaymentsInOrder(
            @Param("agreement") InstallmentAgreementEntity agreement);

    // NEW: Get next incomplete payment
    @Query("SELECT p FROM InstallmentPaymentEntity p " +
            "WHERE p.agreement = :agreement " +
            "AND p.paymentStatus IN ('SCHEDULED', 'PENDING', 'PARTIALLY_PAID', 'LATE') " +
            "ORDER BY p.paymentNumber ASC")
    Optional<InstallmentPaymentEntity> findNextIncompletePayment(
            @Param("agreement") InstallmentAgreementEntity agreement);

    // NEW: Count unpaid/partial payments
    @Query("SELECT COUNT(p) FROM InstallmentPaymentEntity p " +
            "WHERE p.agreement = :agreement " +
            "AND p.paymentStatus IN ('SCHEDULED', 'PENDING', 'PARTIALLY_PAID', 'LATE')")
    long countIncompletePayments(
            @Param("agreement") InstallmentAgreementEntity agreement);
}