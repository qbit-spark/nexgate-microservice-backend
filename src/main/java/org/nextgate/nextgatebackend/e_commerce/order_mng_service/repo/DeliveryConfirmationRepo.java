package org.nextgate.nextgatebackend.e_commerce.order_mng_service.repo;

import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.DeliveryConfirmationEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.ConfirmationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryConfirmationRepo extends JpaRepository<DeliveryConfirmationEntity, UUID> {

    // Find active confirmation for order
    Optional<DeliveryConfirmationEntity> findByOrderAndStatusAndIsRevokedFalse(
            OrderEntity order,
            ConfirmationStatus status
    );

    // Find all confirmations for order (audit trail)
    List<DeliveryConfirmationEntity> findByOrderOrderByGeneratedAtDesc(OrderEntity order);

    // Find pending confirmations that are expired (for cleanup job)
    List<DeliveryConfirmationEntity> findByStatusAndExpiresAtBefore(
            ConfirmationStatus status,
            LocalDateTime dateTime
    );
}