package org.nextgate.nextgatebackend.installment_purchase.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentAgreementRepo extends JpaRepository<InstallmentAgreementEntity, UUID> {

    // Find by agreement number
    Optional<InstallmentAgreementEntity> findByAgreementNumber(String agreementNumber);

    // Find by checkout session
    Optional<InstallmentAgreementEntity> findByCheckoutSessionId(UUID checkoutSessionId);

    // Find customer's agreements
    List<InstallmentAgreementEntity> findByCustomerOrderByCreatedAtDesc(AccountEntity customer);

    // Find customer's agreements by status
    List<InstallmentAgreementEntity> findByCustomerAndAgreementStatusOrderByCreatedAtDesc(
            AccountEntity customer, AgreementStatus status);

    // Find agreements with upcoming payments
    List<InstallmentAgreementEntity> findByAgreementStatusAndNextPaymentDateBetween(
            AgreementStatus status, LocalDateTime startDate, LocalDateTime endDate);

    // Find by shop
    List<InstallmentAgreementEntity> findByShopOrderByCreatedAtDesc(ShopEntity shop);

    // Find by product
    List<InstallmentAgreementEntity> findByProductOrderByCreatedAtDesc(ProductEntity product);

    // Count customer's active agreements
    long countByCustomerAndAgreementStatus(AccountEntity customer, AgreementStatus status);
}