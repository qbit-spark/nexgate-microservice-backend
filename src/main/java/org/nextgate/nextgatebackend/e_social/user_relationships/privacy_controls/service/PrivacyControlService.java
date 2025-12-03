package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.service;


import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.BlockEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.MuteEntity;

import java.util.List;
import java.util.UUID;

public interface PrivacyControlService {

    BlockEntity blockUser(UUID userId);

    void unblockUser(UUID userId);

    MuteEntity muteUser(UUID userId);

    void unmuteUser(UUID userId);

    List<BlockEntity> getBlockedUsers();

    List<MuteEntity> getMutedUsers();

    boolean isBlocked(UUID blockerId, UUID blockedId);

    boolean isMuted(UUID muterId, UUID mutedId);
}