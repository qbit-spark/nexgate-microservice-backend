package org.nextgate.nextgatebackend.notification_system.inapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.notification_system.inapp.payloads.InAppNotificationRequest;
import org.nextgate.nextgatebackend.notification_system.inapp.service.InAppNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/in-app")
@RequiredArgsConstructor
public class InAppNotificationController {

    private final InAppNotificationService notificationService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createNotification(
            @Valid @RequestBody InAppNotificationRequest request) {

        log.info("Received in-app notification request: userId={}, type={}, serviceType={}",
                request.getUserId(), request.getType(), request.getServiceType());

        UUID notificationId = notificationService.saveNotification(request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Notification saved successfully",
                notificationId
        ));
    }
}