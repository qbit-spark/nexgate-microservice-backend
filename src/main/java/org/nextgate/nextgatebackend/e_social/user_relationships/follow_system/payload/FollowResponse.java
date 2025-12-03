package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FollowResponse {
    private UUID id;
    private UUID followerId;
    private UUID followingId;
    private UserSummary follower;
    private UserSummary following;
    private FollowStatus status;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSummary {
        private UUID id;
        private String userName;
        private String firstName;
        private String lastName;
        private List<String> profilePictureUrls;
        private Boolean isVerified;
    }
}