package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.TimelineItemType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TimelineItemResponse {

    private TimelineItemType type; // POST or REPOST
    private PostResponse post; // The actual post content
    private RepostMetadata repostMetadata; // Only if type = REPOST

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RepostMetadata {
        private UUID repostId;
        private UUID reposterId;
        private String reposterUserName;
        private String reposterFirstName;
        private String reposterLastName;
        private String reposterProfilePictureUrl;
        private boolean reposterIsVerified;
        private String repostComment;
        private LocalDateTime repostedAt;
    }
}