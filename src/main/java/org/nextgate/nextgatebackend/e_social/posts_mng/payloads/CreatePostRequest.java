package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostRequest {

    private String content;

    @NotNull(message = "Post type is required")
    private PostType postType;

    private List<MediaRequest> media;

    @Valid
    private PollRequest poll;

    @Valid
    private AttachmentsRequest attachments;

    @Valid
    private CollaborationRequest collaboration;

    @Valid
    private PrivacySettingsRequest privacySettings;

    private LocalDateTime scheduledAt;
}