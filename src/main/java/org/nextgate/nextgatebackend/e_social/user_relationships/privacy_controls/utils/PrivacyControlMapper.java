package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.utils;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.payload.*;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.BlockEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.MuteEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PrivacyControlMapper {

    private final AccountRepo accountRepo;

    public BlockedUserResponse toBlockedUserResponse(BlockEntity block) {
        if (block == null) {
            return null;
        }

        AccountEntity blockedUser = accountRepo.findById(block.getBlockedId()).orElse(null);

        BlockedUserResponse response = new BlockedUserResponse();
        response.setId(block.getId());
        response.setUser(toBlockedUserInfo(blockedUser));
        response.setCreatedAt(block.getCreatedAt());

        return response;
    }

    public List<BlockedUserResponse> toBlockedUserResponseList(List<BlockEntity> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }

        Set<UUID> userIds = blocks.stream()
                .map(BlockEntity::getBlockedId)
                .collect(Collectors.toSet());

        Map<UUID, AccountEntity> usersMap = accountRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, account -> account));

        return blocks.stream()
                .map(block -> {
                    BlockedUserResponse response = new BlockedUserResponse();
                    response.setId(block.getId());
                    response.setUser(toBlockedUserInfo(usersMap.get(block.getBlockedId())));
                    response.setCreatedAt(block.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    public MutedUserResponse toMutedUserResponse(MuteEntity mute) {
        if (mute == null) {
            return null;
        }

        AccountEntity mutedUser = accountRepo.findById(mute.getMutedId()).orElse(null);

        MutedUserResponse response = new MutedUserResponse();
        response.setId(mute.getId());
        response.setUser(toMutedUserInfo(mutedUser));
        response.setCreatedAt(mute.getCreatedAt());

        return response;
    }

    public List<MutedUserResponse> toMutedUserResponseList(List<MuteEntity> mutes) {
        if (mutes == null || mutes.isEmpty()) {
            return List.of();
        }

        Set<UUID> userIds = mutes.stream()
                .map(MuteEntity::getMutedId)
                .collect(Collectors.toSet());

        Map<UUID, AccountEntity> usersMap = accountRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, account -> account));

        return mutes.stream()
                .map(mute -> {
                    MutedUserResponse response = new MutedUserResponse();
                    response.setId(mute.getId());
                    response.setUser(toMutedUserInfo(usersMap.get(mute.getMutedId())));
                    response.setCreatedAt(mute.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private BlockedUserResponse.UserInfo toBlockedUserInfo(AccountEntity account) {
        if (account == null) {
            return null;
        }

        return new BlockedUserResponse.UserInfo(
                account.getId(),
                account.getUserName(),
                account.getFirstName(),
                account.getLastName(),
                account.getProfilePictureUrls(),
                account.getIsVerified()
        );
    }

    private MutedUserResponse.UserInfo toMutedUserInfo(AccountEntity account) {
        if (account == null) {
            return null;
        }

        return new MutedUserResponse.UserInfo(
                account.getId(),
                account.getUserName(),
                account.getFirstName(),
                account.getLastName(),
                account.getProfilePictureUrls(),
                account.getIsVerified()
        );
    }
}