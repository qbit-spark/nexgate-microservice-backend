package org.nextgate.nextgatebackend.notification_system.incoming.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.notification_system.incoming.entity.InAppNotificationEntity;
import org.nextgate.nextgatebackend.notification_system.incoming.payloads.InAppNotificationRequest;
import org.nextgate.nextgatebackend.notification_system.incoming.repo.InAppNotificationRepository;
import org.nextgate.nextgatebackend.notification_system.incoming.service.InAppNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        log.info("Saved in-app notification: id={}, userId={}, type={}",
                saved.getId(), saved.getUserId(), saved.getType());

        return saved.getId();
    }
}