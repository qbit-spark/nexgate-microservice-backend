package org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FeaturedUserResponse {
    private UUID id;
    private String userName;
    private String firstName;
    private String lastName;
    private List<String> profilePictureUrls;
    private Boolean isVerified;
    private long followersCount;
    private boolean followsMe;
}