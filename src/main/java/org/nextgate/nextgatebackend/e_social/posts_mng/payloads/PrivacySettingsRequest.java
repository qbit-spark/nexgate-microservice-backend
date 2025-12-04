package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.CommentPermission;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostVisibility;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.RepostPermission;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PrivacySettingsRequest {

    private PostVisibility visibility = PostVisibility.PUBLIC;

    private CommentPermission whoCanComment = CommentPermission.EVERYONE;

    private RepostPermission whoCanRepost = RepostPermission.EVERYONE;

    private Boolean hideLikesCount = false;

    private Boolean hideCommentsCount = false;
}