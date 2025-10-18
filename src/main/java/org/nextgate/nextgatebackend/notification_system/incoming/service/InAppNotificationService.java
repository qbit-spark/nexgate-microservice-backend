package org.nextgate.nextgatebackend.notification_system.incoming.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.notification_system.incoming.entity.InAppNotificationEntity;
import org.nextgate.nextgatebackend.notification_system.incoming.payloads.InAppNotificationRequest;
import org.nextgate.nextgatebackend.notification_system.incoming.payloads.MarkAsReadRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InAppNotificationService {

    UUID saveNotification(InAppNotificationRequest request);

    Page<InAppNotificationEntity> getMyNotifications(UUID userId, Pageable pageable) throws BadRequestException;

    Long getUnreadCount(UUID userId) throws BadRequestException;

    Page<InAppNotificationEntity> getNotificationsByShop(UUID shopId, UUID userId, Pageable pageable) throws BadRequestException;

    void markAsRead(UUID notificationId, UUID userId) throws BadRequestException, ItemNotFoundException;

    void markMultipleAsRead(MarkAsReadRequest request, UUID userId) throws BadRequestException, RandomExceptions;

    void markAllAsRead(UUID userId) throws BadRequestException;

    void deleteNotification(UUID notificationId, UUID userId) throws BadRequestException, ItemNotFoundException;

    Page<InAppNotificationEntity> getUnreadNotifications(UUID userId, Pageable pageable) throws BadRequestException;

    Page<InAppNotificationEntity> getNotificationsByServiceType(UUID userId, String serviceType, Pageable pageable) throws BadRequestException;

    InAppNotificationEntity getNotificationById(UUID notificationId, UUID userId) throws BadRequestException, ItemNotFoundException;

    Object getNotificationSummary(UUID userId) throws BadRequestException;

    void batchDeleteNotifications(List<UUID> notificationIds, UUID userId) throws BadRequestException, RandomExceptions;

    int deleteAllReadNotifications(UUID userId) throws BadRequestException;
}