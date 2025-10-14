package org.nextgate.nextgatebackend.group_purchase_mng.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupParticipantRepo extends JpaRepository<GroupParticipantEntity, UUID> {

    // Find participant by user and group
    Optional<GroupParticipantEntity> findByUserAndGroupInstance(
            AccountEntity user, GroupPurchaseInstanceEntity groupInstance);

    // Find all participants in a group
    List<GroupParticipantEntity> findByGroupInstanceOrderByJoinedAtAsc(
            GroupPurchaseInstanceEntity groupInstance);

    // ✅ NEW: For explicit eager loading with user relationship
    @Query("SELECT p FROM GroupParticipantEntity p " +
            "JOIN FETCH p.user " +
            "WHERE p.groupInstance = :group " +
            "ORDER BY p.joinedAt ASC")
    List<GroupParticipantEntity> findWithUserByGroup(
            @Param("group") GroupPurchaseInstanceEntity group);

    // Find user's active participations
    List<GroupParticipantEntity> findByUserAndStatusOrderByJoinedAtDesc(
            AccountEntity user, ParticipantStatus status);

    // Find by checkout session
    Optional<GroupParticipantEntity> findByCheckoutSessionId(UUID checkoutSessionId);

    List<GroupParticipantEntity> findByUser(AccountEntity user);
}