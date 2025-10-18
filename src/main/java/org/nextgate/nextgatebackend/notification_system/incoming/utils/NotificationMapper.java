package org.nextgate.nextgatebackend.notification_system.incoming.utils;

import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.notification_system.incoming.entity.InAppNotificationEntity;
import org.nextgate.nextgatebackend.notification_system.incoming.payloads.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NotificationMapper {

    // ========================================
    // SINGLE NOTIFICATION MAPPING
    // ========================================

    public GlobeSuccessResponseBuilder toNotificationResponse(InAppNotificationEntity notification) {

        if (notification == null) {
            return GlobeSuccessResponseBuilder.builder()
                    .message("Notification not found")
                    .data(null)
                    .build();
        }

        NotificationResponse response = mapToNotificationResponse(notification);

        return GlobeSuccessResponseBuilder.builder()
                .message("Notification retrieved successfully")
                .data(response)
                .build();
    }

    // ========================================
    // LIST OF NOTIFICATIONS MAPPING
    // ========================================

    public GlobeSuccessResponseBuilder toNotificationResponseList(List<InAppNotificationEntity> notifications) {

        List<NotificationResponse> notificationResponses = notifications.stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());

        return GlobeSuccessResponseBuilder.builder()
                .message(notificationResponses.isEmpty()
                        ? "No notifications found"
                        : "Notifications retrieved successfully")
                .data(notificationResponses)
                .build();
    }

    // ========================================
    // PAGINATED NOTIFICATIONS MAPPING
    // ========================================

    public GlobeSuccessResponseBuilder toNotificationPageResponse(Page<InAppNotificationEntity> notificationPage) {

        List<NotificationResponse> notificationResponses = notificationPage.getContent().stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());

        var responseData = new Object() {
            public final List<NotificationResponse> notifications = notificationResponses;
            public final int currentPage = notificationPage.getNumber() + 1; // 1-based
            public final int pageSize = notificationPage.getSize();
            public final long totalElements = notificationPage.getTotalElements();
            public final int totalPages = notificationPage.getTotalPages();
            public final boolean hasNext = notificationPage.hasNext();
            public final boolean hasPrevious = notificationPage.hasPrevious();
            public final boolean isFirst = notificationPage.isFirst();
            public final boolean isLast = notificationPage.isLast();
        };

        return GlobeSuccessResponseBuilder.builder()
                .message(notificationResponses.isEmpty()
                        ? "No notifications found"
                        : "Notifications retrieved successfully")
                .data(responseData)
                .build();
    }

    // ========================================
    // HELPER: MAP SINGLE NOTIFICATION
    // ========================================

    private NotificationResponse mapToNotificationResponse(InAppNotificationEntity notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .shopId(notification.getShopId())
                .serviceId(notification.getServiceId())
                .serviceType(notification.getServiceType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .priority(notification.getPriority())
                .isRead(notification.getIsRead())
                .data(notification.getData())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }

    // ========================================
    // MARK AS READ RESPONSE
    // ========================================

    public GlobeSuccessResponseBuilder toMarkAsReadResponse(int count) {
        var responseData = new Object() {
            public final int markedCount = count;
            public final String status = count > 0 ? "SUCCESS" : "NO_CHANGES";
        };

        return GlobeSuccessResponseBuilder.builder()
                .message(count > 0
                        ? String.format("%d notification(s) marked as read", count)
                        : "No notifications were marked as read")
                .data(responseData)
                .build();
    }

    // ========================================
    // UNREAD COUNT RESPONSE
    // ========================================

    public GlobeSuccessResponseBuilder toUnreadCountResponse(Long count) {
        var responseData = new Object() {
            public final Long unreadCount = count;
        };

        return GlobeSuccessResponseBuilder.builder()
                .message("Unread count retrieved successfully")
                .data(responseData)
                .build();
    }
}