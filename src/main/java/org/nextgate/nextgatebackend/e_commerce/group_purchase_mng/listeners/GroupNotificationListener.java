package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.events.*;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupParticipantRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.GroupNotificationMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * Handles notification events for group purchases.
 *
 * CRITICAL: Executes AFTER transaction commits (AFTER_COMMIT phase).
 * This ensures:
 * 1. Database changes are persisted before notifications sent
 * 2. Notification failures don't rollback database transactions
 * 3. Notifications are sent asynchronously (non-blocking)
 *
 * NOTE: No @Transactional needed - repository methods handle transactions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupNotificationListener {

    private final NotificationPublisher notificationPublisher;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    private final GroupParticipantRepo groupParticipantRepo;

    // ========================================
    // GROUP CREATED NOTIFICATION
    // ========================================

    /**
     * Send notification when group is created.
     * Runs AFTER transaction commits - notification failures won't rollback.
     *
     * No @Transactional needed - findByIdWithRelations() runs in repository's transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onGroupCreated(GroupCreatedNotificationEvent event) {

        log.info("📧 Sending group created notification");
        log.info("  Group: {} ({})", event.getGroupCode(), event.getGroupInstanceId());

            // Validate shop owner exists
            if (event.getShopOwnerId() == null) {
                log.warn("⚠️ Cannot send notification - shop has no owner");
                return;
            }

            // Fetch group with all relationships eagerly loaded
            // Repository method runs in its own transaction
            GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                    .findByIdWithRelations(event.getGroupInstanceId())
                    .orElse(null);

            if (group == null) {
                log.error("Group not found: {}", event.getGroupInstanceId());
                return;
            }

            log.debug("✓ Group loaded with relations");

            // Prepare notification data
            Map<String, Object> data = GroupNotificationMapper.mapNewGroupCreated(
                    group,
                    group.getInitiator(),
                    event.getInitialSeats()
            );

            // Build recipient (shop owner)
            Recipient recipient = Recipient.builder()
                    .userId(event.getShopOwnerId().toString())
                    .email(event.getShopOwnerEmail())
                    .phone(event.getShopOwnerPhone())
                    .name(event.getShopOwnerName())
                    .language("en")
                    .build();

            // Create notification event
            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.GROUP_PURCHASE_CREATED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            // Publish notification
            notificationPublisher.publish(notification);

            log.info("✅ Group created notification sent successfully");

    }

    // ========================================
    // MEMBER JOINED NOTIFICATION
    // ========================================

    /**
     * Send notifications when member joins group.
     * Notifies: 1) Shop owner, 2) Existing members
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onMemberJoined(MemberJoinedNotificationEvent event) {

        log.info("📧 Sending member joined notifications");
        log.info("  Group: {} ({})", event.getGroupCode(), event.getGroupInstanceId());
        log.info("  New Member: {}", event.getNewMemberName());

        try {
            // Fetch group with relationships
            GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                    .findByIdWithRelations(event.getGroupInstanceId())
                    .orElse(null);

            if (group == null) {
                log.error("Group not found: {}", event.getGroupInstanceId());
                return;
            }

            // 1. Notify shop owner
            notifyShopOwnerAboutNewMember(event, group);

            // 2. Notify existing members
            notifyExistingMembersAboutNewJoin(event, group);

            log.info("✅ Member joined notifications sent successfully");

        } catch (Exception e) {
            log.error("❌ Failed to send member joined notifications", e);
            log.error("  Group: {}", event.getGroupInstanceId());
        }
    }

    // ========================================
    // SEATS TRANSFERRED NOTIFICATION
    // ========================================

    /**
     * Send notification when seats transferred between groups.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onSeatsTransferred(SeatsTransferredNotificationEvent event) {

        log.info("📧 Sending seats transferred notification");
        log.info("  User: {}", event.getUserName());
        log.info("  From: {} → To: {}", event.getSourceGroupCode(), event.getTargetGroupCode());

        try {
            // Fetch groups with relationships
            GroupPurchaseInstanceEntity sourceGroup = groupPurchaseInstanceRepo
                    .findByIdWithRelations(event.getSourceGroupId())
                    .orElse(null);

            GroupPurchaseInstanceEntity targetGroup = groupPurchaseInstanceRepo
                    .findByIdWithRelations(event.getTargetGroupId())
                    .orElse(null);

            if (sourceGroup == null || targetGroup == null) {
                log.error("Source or target group not found");
                return;
            }

            // Prepare notification data
            Map<String, Object> data = GroupNotificationMapper.mapSeatsTransferred(
                    sourceGroup,
                    targetGroup,
                    targetGroup.getInitiator(),
                    event.getQuantity()
            );

            // Build recipient
            Recipient recipient = Recipient.builder()
                    .userId(event.getUserId().toString())
                    .email(event.getUserEmail())
                    .name(event.getUserName())
                    .language("en")
                    .build();

            // Create notification event
            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.GROUP_SEATS_TRANSFERRED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.PUSH,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            // Publish notification
            notificationPublisher.publish(notification);

            log.info("✅ Seats transferred notification sent successfully");

        } catch (Exception e) {
            log.error("❌ Failed to send seats transferred notification", e);
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private void notifyShopOwnerAboutNewMember(
            MemberJoinedNotificationEvent event,
            GroupPurchaseInstanceEntity group) {

        try {
            if (event.getShopOwnerId() == null) {
                log.warn("Cannot notify shop owner - no owner");
                return;
            }

            Map<String, Object> data = GroupNotificationMapper.mapMemberJoinedForShopOwner(
                    group,
                    group.getInitiator(),
                    event.getQuantity()
            );

            Recipient recipient = Recipient.builder()
                    .userId(event.getShopOwnerId().toString())
                    .email(event.getShopOwnerEmail())
                    .name(event.getShopOwnerName())
                    .language("en")
                    .build();

            NotificationEvent notification = NotificationEvent.builder()
                    .type(NotificationType.GROUP_MEMBER_JOINED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            notificationPublisher.publish(notification);

            log.info("✓ Shop owner notified");

        } catch (Exception e) {
            log.error("Failed to notify shop owner", e);
        }
    }

    private void notifyExistingMembersAboutNewJoin(
            MemberJoinedNotificationEvent event,
            GroupPurchaseInstanceEntity group) {

        try {
            // Get active participants except new member
            List<GroupParticipantEntity> existingParticipants = groupParticipantRepo
                    .findWithUserByGroup(group)
                    .stream()
                    .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                    .filter(p -> !p.getUser().getId().equals(event.getNewMemberId()))
                    .toList();

            if (existingParticipants.isEmpty()) {
                log.info("No existing members to notify");
                return;
            }

            log.info("Notifying {} existing members", existingParticipants.size());

            for (GroupParticipantEntity participant : existingParticipants) {
                try {
                    notifyExistingMember(event, group, participant);
                } catch (Exception e) {
                    log.error("Failed to notify member: {}",
                            participant.getUser().getUserName(), e);
                }
            }

        } catch (Exception e) {
            log.error("Failed to notify existing members", e);
        }
    }

    private void notifyExistingMember(
            MemberJoinedNotificationEvent event,
            GroupPurchaseInstanceEntity group,
            GroupParticipantEntity participant) {

        Map<String, Object> data = GroupNotificationMapper.mapMemberJoinedForExistingMembers(
                group,
                participant.getUser(),
                group.getInitiator(),
                event.getQuantity()
        );

        Recipient recipient = Recipient.builder()
                .userId(participant.getUser().getId().toString())
                .email(participant.getUser().getEmail())
                .name(participant.getUser().getFirstName())
                .language("en")
                .build();

        NotificationEvent notification = NotificationEvent.builder()
                .type(NotificationType.GROUP_MEMBER_JOINED)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.PUSH,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.LOW)
                .data(data)
                .build();

        notificationPublisher.publish(notification);
    }
}