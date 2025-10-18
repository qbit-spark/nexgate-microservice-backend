package org.nextgate.nextgatebackend.notification_system.general;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.notification_system.incoming.entity.InAppNotificationEntity;
import org.nextgate.nextgatebackend.notification_system.incoming.payloads.MarkAsReadRequest;
import org.nextgate.nextgatebackend.notification_system.incoming.service.InAppNotificationService;
import org.nextgate.nextgatebackend.notification_system.incoming.utils.NotificationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final AccountRepo accountRepo;

    @GetMapping("/me")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<InAppNotificationEntity> notifications = notificationService.getMyNotifications(userId, pageable);
        return ResponseEntity.ok(notificationMapper.toNotificationPageResponse(notifications));
    }

    @GetMapping("/unread")
    public ResponseEntity<GlobeSuccessResponseBuilder> getUnreadNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<InAppNotificationEntity> notifications = notificationService.getUnreadNotifications(userId, pageable);
        return ResponseEntity.ok(notificationMapper.toNotificationPageResponse(notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<GlobeSuccessResponseBuilder> getUnreadCount() throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        Long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(notificationMapper.toUnreadCountResponse(count));
    }

    @GetMapping("/summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getNotificationSummary() throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        var summary = notificationService.getNotificationSummary(userId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Notification summary retrieved successfully", summary));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getNotificationsByShop(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<InAppNotificationEntity> notifications = notificationService.getNotificationsByShop(shopId, userId, pageable);
        return ResponseEntity.ok(notificationMapper.toNotificationPageResponse(notifications));
    }

    @GetMapping("/service/{serviceType}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getNotificationsByServiceType(
            @PathVariable String serviceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<InAppNotificationEntity> notifications = notificationService.getNotificationsByServiceType(userId, serviceType, pageable);
        return ResponseEntity.ok(notificationMapper.toNotificationPageResponse(notifications));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getNotificationById(
            @PathVariable UUID notificationId) throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        InAppNotificationEntity notification = notificationService.getNotificationById(notificationId, userId);

        return ResponseEntity.ok(notificationMapper.toNotificationResponse(notification));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<GlobeSuccessResponseBuilder> markAsRead(
            @PathVariable UUID notificationId) throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        notificationService.markAsRead(notificationId, userId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Notification marked as read", null));
    }

    @PutMapping("/read")
    public ResponseEntity<GlobeSuccessResponseBuilder> markMultipleAsRead(
            @Valid @RequestBody MarkAsReadRequest request) throws ItemNotFoundException, BadRequestException, RandomExceptions {

        UUID userId = getAuthenticatedAccount().getAccountId();
        notificationService.markMultipleAsRead(request, userId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                String.format("%d notification(s) marked as read", request.getNotificationIds().size()), null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<GlobeSuccessResponseBuilder> markAllAsRead() throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "All notifications marked as read", null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteNotification(
            @PathVariable UUID notificationId) throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        notificationService.deleteNotification(notificationId, userId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Notification deleted successfully", null));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<GlobeSuccessResponseBuilder> batchDeleteNotifications(
            @Valid @RequestBody MarkAsReadRequest request) throws ItemNotFoundException, BadRequestException, RandomExceptions {

        UUID userId = getAuthenticatedAccount().getAccountId();
        notificationService.batchDeleteNotifications(request.getNotificationIds(), userId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                String.format("%d notification(s) deleted successfully", request.getNotificationIds().size()), null));
    }

    @DeleteMapping("/read")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteAllReadNotifications() throws ItemNotFoundException, BadRequestException {

        UUID userId = getAuthenticatedAccount().getAccountId();
        int deletedCount = notificationService.deleteAllReadNotifications(userId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                String.format("%d read notification(s) deleted successfully", deletedCount), deletedCount));
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}