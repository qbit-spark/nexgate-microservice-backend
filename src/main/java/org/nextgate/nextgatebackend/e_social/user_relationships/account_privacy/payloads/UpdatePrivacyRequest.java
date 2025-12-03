package org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.payloads;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePrivacyRequest {

    @NotNull(message = "isPrivate field is required")
    private Boolean isPrivate;
}
