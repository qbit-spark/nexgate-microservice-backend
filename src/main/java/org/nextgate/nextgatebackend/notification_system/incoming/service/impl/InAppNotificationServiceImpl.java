package org.nextgate.nextgatebackend.notification_system.incoming.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.notification_system.incoming.entity.InAppNotificationEntity;
import org.nextgate.nextgatebackend.notification_system.incoming.payloads.InAppNotificationRequest;
import org.nextgate.nextgatebackend.notification_system.incoming.payloads.MarkAsReadRequest;
import org.nextgate.nextgatebackend.notification_system.incoming.repo.InAppNotificationRepository;
import org.nextgate.nextgatebackend.notification_system.incoming.service.InAppNotificationService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationServiceImpl implements InAppNotificationService {

    private final InAppNotificationRepository notificationRepository;

    @Override
    @Transactional
    public UUID saveNotification(InAppNotificationRequest request) {
        InAppNotificationEntity entity = InAppNotificationEntity.builder()
                .userId(request.getUserId())
                .shopId(request.getShopId())
                .serviceId(request.getServiceId())
                .serviceType(request.getServiceType())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .priority(request.getPriority())
                .data(request.getData())
                .build();

        InAppNotificationEntity saved = notificationRepository.save(entity);
        log.info("Saved notification: id={}, userId={}, type={}", saved.getId(), saved.getUserId(), saved.getType());
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InAppNotificationEntity> getMyNotifications(UUID userId, Pageable pageable) throws BadRequestException {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(UUID userId) throws BadRequestException {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InAppNotificationEntity> getNotificationsByShop(UUID shopId, UUID userId, Pageable pageable) throws BadRequestException {
        if (shopId == null || userId == null) {
            throw new BadRequestException("Shop ID and User ID cannot be null");
        }
        return notificationRepository.findByShopIdOrderByCreatedAtDesc(shopId, pageable);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) throws BadRequestException, ItemNotFoundException {
        if (notificationId == null || userId == null) {
            throw new BadRequestException("Notification ID and User ID cannot be null");
        }

        InAppNotificationEntity notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ItemNotFoundException("Notification not found or access denied"));

        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markMultipleAsRead(MarkAsReadRequest request, UUID userId) throws BadRequestException, RandomExceptions {
        if (request == null || request.getNotificationIds() == null || request.getNotificationIds().isEmpty()) {
            throw new BadRequestException("Notification IDs cannot be empty");
        }
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        List<UUID> notificationIds = request.getNotificationIds();
        List<InAppNotificationEntity> notifications = notificationRepository.findByIdInAndUserId(notificationIds, userId);

        if (notifications.size() != notificationIds.size()) {
            throw new RandomExceptions("Some notifications not found or access denied");
        }

        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(now);
        });

        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) throws BadRequestException {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        List<InAppNotificationEntity> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalse(userId);

        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(now);
        });

        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) throws BadRequestException, ItemNotFoundException {
        if (notificationId == null || userId == null) {
            throw new BadRequestException("Notification ID and User ID cannot be null");
        }

        InAppNotificationEntity notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ItemNotFoundException("Notification not found or access denied"));

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InAppNotificationEntity> getUnreadNotifications(UUID userId, Pageable pageable) throws BadRequestException {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InAppNotificationEntity> getNotificationsByServiceType(UUID userId, String serviceType, Pageable pageable) throws BadRequestException {
        if (userId == null || serviceType == null || serviceType.trim().isEmpty()) {
            throw new BadRequestException("User ID and Service type cannot be null or empty");
        }
        return notificationRepository.findByUserIdAndServiceTypeOrderByCreatedAtDesc(userId, serviceType, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public InAppNotificationEntity getNotificationById(UUID notificationId, UUID userId) throws BadRequestException, ItemNotFoundException {
        if (notificationId == null || userId == null) {
            throw new BadRequestException("Notification ID and User ID cannot be null");
        }
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ItemNotFoundException("Notification not found or access denied"));
    }

    @Override
    @Transactional(readOnly = true)
    public Object getNotificationSummary(UUID userId) throws BadRequestException {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        Long totalCount = notificationRepository.countByUserId(userId);
        Long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        Long readCount = totalCount - unreadCount;

        return new Object() {
            public final Long total = totalCount;
            public final Long unread = unreadCount;
            public final Long read = readCount;
        };
    }

    @Override
    @Transactional
    public void batchDeleteNotifications(List<UUID> notificationIds, UUID userId) throws BadRequestException, RandomExceptions {
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new BadRequestException("Notification IDs cannot be empty");
        }
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        List<InAppNotificationEntity> notifications = notificationRepository.findByIdInAndUserId(notificationIds, userId);

        if (notifications.size() != notificationIds.size()) {
            throw new RandomExceptions("Some notifications not found or access denied");
        }

        notificationRepository.deleteAll(notifications);
    }

    @Override
    @Transactional
    public int deleteAllReadNotifications(UUID userId) throws BadRequestException {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        List<InAppNotificationEntity> readNotifications = notificationRepository.findByUserIdAndIsReadTrue(userId);
        notificationRepository.deleteAll(readNotifications);

        return readNotifications.size();
    }
}