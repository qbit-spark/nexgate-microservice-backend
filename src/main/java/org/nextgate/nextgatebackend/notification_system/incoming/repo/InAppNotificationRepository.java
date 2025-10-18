package org.nextgate.nextgatebackend.notification_system.incoming.repo;

import org.nextgate.nextgatebackend.notification_system.incoming.entity.InAppNotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, UUID> {

    Page<InAppNotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<InAppNotificationEntity> findByShopIdOrderByCreatedAtDesc(UUID shopId, Pageable pageable);

    Optional<InAppNotificationEntity> findByIdAndUserId(UUID id, UUID userId);

    Long countByUserIdAndIsReadFalse(UUID userId);

    Long countByUserId(UUID userId);

    List<InAppNotificationEntity> findByIdInAndUserId(List<UUID> ids, UUID userId);

    Page<InAppNotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<InAppNotificationEntity> findByUserIdAndIsReadFalse(UUID userId);

    List<InAppNotificationEntity> findByUserIdAndIsReadTrue(UUID userId);

    Page<InAppNotificationEntity> findByUserIdAndServiceTypeOrderByCreatedAtDesc(UUID userId, String serviceType, Pageable pageable);
}