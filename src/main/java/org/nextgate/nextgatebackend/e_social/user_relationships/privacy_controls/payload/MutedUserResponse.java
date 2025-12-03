package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MutedUserResponse {
    private UUID id;
    private UserInfo user;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String userName;
        private String firstName;
        private String lastName;
        private List<String> profilePictureUrls;
        private Boolean isVerified;
    }
}