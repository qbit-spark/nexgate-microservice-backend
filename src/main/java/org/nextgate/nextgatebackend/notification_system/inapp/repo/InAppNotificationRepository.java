package org.nextgate.nextgatebackend.notification_system.inapp.repo;

import org.nextgate.nextgatebackend.notification_system.inapp.entity.InAppNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, UUID> {
}