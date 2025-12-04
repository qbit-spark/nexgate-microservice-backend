package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.ImageTagType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageTagData {

    private String id;
    private ImageTagType tagType;
    private String taggedUserId;
    private String taggedProductId;
    private String taggedShopId;
    private double xPosition;
    private double yPosition;
}
