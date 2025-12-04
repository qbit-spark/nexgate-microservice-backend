package org.nextgate.nextgatebackend.e_social.posts.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.ImageTagType;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageTagRequest {

    @NotNull(message = "Tag type is required")
    private ImageTagType tagType;

    private UUID taggedUserId;

    private UUID taggedProductId;

    private UUID taggedShopId;

    @NotNull(message = "X position is required")
    private Double xPosition;

    @NotNull(message = "Y position is required")
    private Double yPosition;
}