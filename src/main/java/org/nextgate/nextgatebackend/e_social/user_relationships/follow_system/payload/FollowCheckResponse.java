package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FollowCheckResponse {
    private UUID userId;
    private boolean isFollowing;
}