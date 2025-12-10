package org.nextgate.nextgatebackend.e_social.posts_mng.utils;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostVisibility;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo.FollowRepository;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.repo.BlockRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostVisibilityUtil {

    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;

    public boolean canViewPost(PostEntity post, UUID viewerId) {
        UUID authorId = post.getAuthorId();

        if (viewerId != null && authorId.equals(viewerId)) {
            return true;
        }

        if (viewerId != null) {
            if (blockRepository.existsByBlockerIdAndBlockedId(authorId, viewerId)) {
                return false;
            }
            if (blockRepository.existsByBlockerIdAndBlockedId(viewerId, authorId)) {
                return false;
            }
        }

        if (post.getVisibility() == PostVisibility.FOLLOWERS) {
            if (viewerId == null) {
                return false;
            }
            return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                    viewerId, authorId, FollowStatus.ACCEPTED
            );
        }

        return true;
    }
}