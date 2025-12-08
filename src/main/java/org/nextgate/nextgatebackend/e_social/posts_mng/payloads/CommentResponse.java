package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {

    private UUID id;
    private UUID postId;
    private UUID parentCommentId;
    private Author author;
    private String content;
    private ContentParsed contentParsed;
    private int likesCount;
    private int repliesCount;
    private boolean isPinned;
    private boolean hasLiked;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canPin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Author {
        private UUID id;
        private String userName;
        private String firstName;
        private String lastName;
        private String profilePictureUrl;
        private boolean isVerified;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContentParsed {
        private String text;
        private List<MentionEntity> mentions = new ArrayList<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MentionEntity {
        private UUID userId;
        private String userName;
        private String displayName;
        private int startIndex;
        private int endIndex;
    }
}