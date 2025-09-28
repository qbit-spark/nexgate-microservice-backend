package org.nextgate.nextgatebackend.payment_methods.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodsEntity, UUID> {

    // Basic queries
    List<PaymentMethodsEntity> findByOwnerAndIsActiveTrue(AccountEntity owner);

    List<PaymentMethodsEntity> findByOwner(AccountEntity owner);

    List<PaymentMethodsEntity> findByOwnerAndIsDefaultTrue(AccountEntity owner);

    Optional<PaymentMethodsEntity> findByPaymentMethodIdAndOwner(UUID paymentMethodId, AccountEntity owner);

    // Count queries for duplicate checking - we'll handle this in service layer
    List<PaymentMethodsEntity> findByOwnerAndPaymentMethodType(AccountEntity owner, PaymentMethodsType paymentMethodType);

    boolean existsByOwnerAndPaymentMethodType(AccountEntity owner, PaymentMethodsType paymentMethodType);
}