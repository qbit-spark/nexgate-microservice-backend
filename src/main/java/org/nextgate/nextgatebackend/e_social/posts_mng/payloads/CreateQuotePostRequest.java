package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class CreateQuotePostRequest {

    @NotNull(message = "Content cannot be null")
    private String content;

    private List<MediaRequest> media;

    @Valid
    private AttachmentsRequest attachments;

    @Valid
    private PrivacySettingsRequest privacySettings;

    private LocalDateTime scheduledAt;

}