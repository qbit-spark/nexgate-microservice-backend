package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

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
public class MediaData {

    private String id;
    private MediaType mediaType;
    private String originalUrl;
    private String thumbnailUrl;
    private String placeholderBase64;
    private Integer width;
    private Integer height;
    private Integer duration;
    private int order;
    private List<ImageTagData> imageTags;
}