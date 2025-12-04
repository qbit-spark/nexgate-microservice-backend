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
public class FollowingResponse {
    private UUID id;
    private UserInfo user;
    private FollowStatus status;
    private LocalDateTime createdAt;

}