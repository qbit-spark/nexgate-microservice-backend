package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.utils;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload.*;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.entity.FollowEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FollowMapper {

    private final AccountRepo accountRepo;

    public FollowResponse toResponse(FollowEntity entity) {
        if (entity == null) {
            return null;
        }

        FollowResponse response = new FollowResponse();
        response.setId(entity.getId());
        response.setFollowerId(entity.getFollowerId());
        response.setFollowingId(entity.getFollowingId());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());

        AccountEntity follower = accountRepo.findById(entity.getFollowerId()).orElse(null);
        AccountEntity following = accountRepo.findById(entity.getFollowingId()).orElse(null);

        response.setFollower(toUserSummary(follower));
        response.setFollowing(toUserSummary(following));

        return response;
    }

    public List<FollowResponse> toResponseList(List<FollowEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        Set<UUID> userIds = entities.stream()
                .flatMap(e -> List.of(e.getFollowerId(), e.getFollowingId()).stream())
                .collect(Collectors.toSet());

        Map<UUID, AccountEntity> usersMap = accountRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, account -> account));

        return entities.stream()
                .map(entity -> toResponseWithCache(entity, usersMap))
                .collect(Collectors.toList());
    }

    private FollowResponse toResponseWithCache(FollowEntity entity, Map<UUID, AccountEntity> usersMap) {
        FollowResponse response = new FollowResponse();
        response.setId(entity.getId());
        response.setFollowerId(entity.getFollowerId());
        response.setFollowingId(entity.getFollowingId());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());

        response.setFollower(toUserSummary(usersMap.get(entity.getFollowerId())));
        response.setFollowing(toUserSummary(usersMap.get(entity.getFollowingId())));

        return response;
    }

    private FollowResponse.UserSummary toUserSummary(AccountEntity account) {
        if (account == null) {
            return null;
        }

        return new FollowResponse.UserSummary(
                account.getId(),
                account.getUserName(),
                account.getFirstName(),
                account.getLastName(),
                account.getProfilePictureUrls(),
                account.getIsVerified()
        );
    }

    public FollowerResponse toFollowerResponse(FollowEntity entity) {
        if (entity == null) {
            return null;
        }

        AccountEntity follower = accountRepo.findById(entity.getFollowerId()).orElse(null);

        FollowerResponse response = new FollowerResponse();
        response.setId(entity.getId());
        response.setUser(toUserInfo(follower));
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }

    public List<FollowerResponse> toFollowerResponseList(List<FollowEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        Set<UUID> followerIds = entities.stream()
                .map(FollowEntity::getFollowerId)
                .collect(Collectors.toSet());

        Map<UUID, AccountEntity> usersMap = accountRepo.findAllById(followerIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, account -> account));

        return entities.stream()
                .map(entity -> {
                    FollowerResponse response = new FollowerResponse();
                    response.setId(entity.getId());
                    response.setUser(toUserInfo(usersMap.get(entity.getFollowerId())));
                    response.setStatus(entity.getStatus());
                    response.setCreatedAt(entity.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    public FollowingResponse toFollowingResponse(FollowEntity entity) {
        if (entity == null) {
            return null;
        }

        AccountEntity following = accountRepo.findById(entity.getFollowingId()).orElse(null);

        FollowingResponse response = new FollowingResponse();
        response.setId(entity.getId());
        response.setUser(toUserInfo(following));
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }

    public List<FollowingResponse> toFollowingResponseList(List<FollowEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        Set<UUID> followingIds = entities.stream()
                .map(FollowEntity::getFollowingId)
                .collect(Collectors.toSet());

        Map<UUID, AccountEntity> usersMap = accountRepo.findAllById(followingIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, account -> account));

        return entities.stream()
                .map(entity -> {
                    FollowingResponse response = new FollowingResponse();
                    response.setId(entity.getId());
                    response.setUser(toUserInfo(usersMap.get(entity.getFollowingId())));
                    response.setStatus(entity.getStatus());
                    response.setCreatedAt(entity.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private UserInfo toUserInfo(AccountEntity account) {
        if (account == null) {
            return null;
        }

        return new UserInfo(
                account.getId(),
                account.getUserName(),
                account.getFirstName(),
                account.getLastName(),
                account.getProfilePictureUrls(),
                account.getIsVerified()
        );
    }
}