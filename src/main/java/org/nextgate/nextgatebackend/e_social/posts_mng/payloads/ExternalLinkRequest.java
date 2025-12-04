package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExternalLinkRequest {

    @NotBlank(message = "URL is required")
    private String url;
}