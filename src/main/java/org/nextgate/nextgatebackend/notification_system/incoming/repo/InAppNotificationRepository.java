package org.nextgate.nextgatebackend.notification_system.incoming.repo;

import org.nextgate.nextgatebackend.notification_system.incoming.entity.InAppNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, UUID> {
}