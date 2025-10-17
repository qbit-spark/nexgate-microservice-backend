package org.nextgate.nextgatebackend.notification_system.incoming.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "in_app_notifications", indexes = {
        @Index(name = "idx_inapp_user_id", columnList = "user_id"),
        @Index(name = "idx_inapp_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_inapp_created_at", columnList = "created_at"),
        @Index(name = "idx_inapp_shop_id", columnList = "shop_id"),
        @Index(name = "idx_inapp_service_type", columnList = "service_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "shop_id")
    private UUID shopId;

    @Column(name = "service_id", nullable = false, length = 100)
    private String serviceId;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
    }
}