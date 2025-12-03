package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.FollowCheckResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.UserStatsResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.entity.FollowEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo.FollowRepository;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.service.FollowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public FollowEntity followUser(UUID followingId) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID followerId = authenticatedUser.getId();

        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        AccountEntity userToFollow = accountRepo.findById(followingId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("Already following this user");
        }

        FollowEntity follow = new FollowEntity();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setStatus(FollowStatus.ACCEPTED);
        follow.setCreatedAt(LocalDateTime.now());

        return followRepository.save(follow);
    }

    @Override
    @Transactional
    public void unfollowUser(UUID followingId) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID followerId = authenticatedUser.getId();

        accountRepo.findById(followingId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    @Transactional
    public FollowEntity acceptFollowRequest(UUID followId) {
        FollowEntity follow = followRepository.findById(followId)
                .orElseThrow(() -> new IllegalArgumentException("Follow request not found"));

        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new IllegalStateException("Follow request is not pending");
        }

        follow.setStatus(FollowStatus.ACCEPTED);
        return followRepository.save(follow);
    }

    @Override
    @Transactional
    public void declineFollowRequest(UUID followId) {
        FollowEntity follow = followRepository.findById(followId)
                .orElseThrow(() -> new IllegalArgumentException("Follow request not found"));

        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new IllegalStateException("Follow request is not pending");
        }

        followRepository.delete(follow);
    }

    @Override
    public List<FollowEntity> getFollowers(UUID userId) {
        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return followRepository.findByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED);
    }

    @Override
    public List<FollowEntity> getFollowing(UUID userId) {
        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return followRepository.findByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED);
    }

    @Override
    public List<FollowEntity> getPendingRequests() {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        return followRepository.findByFollowingIdAndStatusOrderByCreatedAtDesc(authenticatedUser.getId(), FollowStatus.PENDING);
    }

    @Override
    public Page<FollowEntity> getFollowersPaged(UUID userId, Pageable pageable) {
        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return followRepository.findByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED, pageable);
    }

    @Override
    public Page<FollowEntity> getFollowingPaged(UUID userId, Pageable pageable) {
        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return followRepository.findByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED, pageable);
    }

    @Override
    public UserStatsResponse getUserStats(UUID userId) {
        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long followersCount = followRepository.countByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED);
        long followingCount = followRepository.countByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED);
        long pendingCount = followRepository.countByFollowingIdAndStatus(userId, FollowStatus.PENDING);

        return new UserStatsResponse(userId, followersCount, followingCount, pendingCount);
    }

    @Override
    public FollowCheckResponse checkFollowStatus(UUID userId) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(authenticatedUser.getId(), userId);

        return new FollowCheckResponse(userId, isFollowing);
    }

    private AccountEntity getAuthenticatedAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}