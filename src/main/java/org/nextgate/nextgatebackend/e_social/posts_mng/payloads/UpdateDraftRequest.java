package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDraftRequest {

    private String content;

    private List<MediaRequest> media;

    @Valid
    private PrivacySettingsRequest privacySettings;
}