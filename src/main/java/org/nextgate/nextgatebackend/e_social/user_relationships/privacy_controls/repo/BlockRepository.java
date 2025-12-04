package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.repo;

import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<BlockEntity, UUID> {

    Optional<BlockEntity> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<BlockEntity> findByBlockerIdOrderByCreatedAtDesc(UUID blockerId);

    List<BlockEntity> findByBlockedId(UUID blockedId);
}