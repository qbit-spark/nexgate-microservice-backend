package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo;

import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.entity.FollowEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, UUID> {

    Optional<FollowEntity> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    List<FollowEntity> findByFollowingIdAndStatus(UUID followingId, FollowStatus status);

    List<FollowEntity> findByFollowerIdAndStatus(UUID followerId, FollowStatus status);

    Page<FollowEntity> findByFollowingIdAndStatus(UUID followingId, FollowStatus status, Pageable pageable);

    Page<FollowEntity> findByFollowerIdAndStatus(UUID followerId, FollowStatus status, Pageable pageable);

    long countByFollowingIdAndStatus(UUID followingId, FollowStatus status);

    long countByFollowerIdAndStatus(UUID followerId, FollowStatus status);

    List<FollowEntity> findByFollowingIdAndStatusOrderByCreatedAtDesc(UUID followingId, FollowStatus status);
}