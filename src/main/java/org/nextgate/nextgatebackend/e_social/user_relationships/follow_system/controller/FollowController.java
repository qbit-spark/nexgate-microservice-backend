package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.controller;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.FollowCheckResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.FollowResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.UserStatsResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.entity.FollowEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.service.FollowService;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.utils.FollowMapper;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/e-social/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final FollowMapper followMapper;

    @PostMapping("/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> followUser(@PathVariable UUID userId) {
        FollowEntity follow = followService.followUser(userId);
        FollowResponse response = followMapper.toResponse(follow);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User followed successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> unfollowUser(@PathVariable UUID userId) {
        followService.unfollowUser(userId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User unfollowed successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/requests/{followId}/accept")
    public ResponseEntity<GlobeSuccessResponseBuilder> acceptFollowRequest(@PathVariable UUID followId) {
        FollowEntity follow = followService.acceptFollowRequest(followId);
        FollowResponse response = followMapper.toResponse(follow);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Follow request accepted",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/requests/{followId}/decline")
    public ResponseEntity<GlobeSuccessResponseBuilder> declineFollowRequest(@PathVariable UUID followId) {
        followService.declineFollowRequest(followId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Follow request declined",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getFollowers(@PathVariable UUID userId) {
        List<FollowEntity> followers = followService.getFollowers(userId);
        List<FollowResponse> response = followMapper.toResponseList(followers);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Followers retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getFollowing(@PathVariable UUID userId) {
        List<FollowEntity> following = followService.getFollowing(userId);
        List<FollowResponse> response = followMapper.toResponseList(following);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Following retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/followers/{userId}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getFollowersPaged(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FollowEntity> followersPage = followService.getFollowersPaged(userId, pageable);
        Page<FollowResponse> response = followersPage.map(followMapper::toResponse);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Followers retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/following/{userId}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getFollowingPaged(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FollowEntity> followingPage = followService.getFollowingPaged(userId, pageable);
        Page<FollowResponse> response = followingPage.map(followMapper::toResponse);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Following retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPendingRequests() {
        List<FollowEntity> pendingRequests = followService.getPendingRequests();
        List<FollowResponse> response = followMapper.toResponseList(pendingRequests);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Pending requests retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getUserStats(@PathVariable UUID userId) {
        UserStatsResponse stats = followService.getUserStats(userId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User stats retrieved successfully",
                stats
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkFollowStatus(@PathVariable UUID userId) {
        FollowCheckResponse status = followService.checkFollowStatus(userId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Follow status checked",
                status
        );

        return ResponseEntity.ok(successResponse);
    }
}