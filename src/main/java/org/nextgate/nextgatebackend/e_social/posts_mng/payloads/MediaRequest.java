package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.MediaType;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MediaRequest {

    @NotNull(message = "Media type is required")
    private MediaType mediaType;

    @NotNull(message = "Media URL is required")
    private String mediaUrl;

    private String placeholderBase64;

    private Integer width;

    private Integer height;

    private Integer duration;

    private List<ImageTagRequest> imageTags;
}