package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.service;


import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.entity.FollowEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.FeaturedUserResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.FollowCheckResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.UserStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface FollowService {

    FollowEntity followUser(UUID followingId);

    void unfollowUser(UUID followingId);

    FollowEntity acceptFollowRequest(UUID followId);

    void declineFollowRequest(UUID followId);

    List<FollowEntity> getFollowers(UUID userId);

    List<FollowEntity> getFollowing(UUID userId);

    Page<FollowEntity> getFollowersPaged(UUID userId, Pageable pageable);

    Page<FollowEntity> getFollowingPaged(UUID userId, Pageable pageable);

    List<FollowEntity> getPendingRequests();

    UserStatsResponse getUserStats(UUID userId);

    FollowCheckResponse checkFollowStatus(UUID userId);

    List<FeaturedUserResponse> getFeaturedUsers(int limit);

    Page<FeaturedUserResponse> getFeaturedUsersPaged(Pageable pageable);
}