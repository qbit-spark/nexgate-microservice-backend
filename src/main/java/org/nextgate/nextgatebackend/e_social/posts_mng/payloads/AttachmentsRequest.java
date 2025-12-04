package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentsRequest {

    private List<UUID> productIds;

    private List<UUID> shopIds;

    private List<UUID> buyTogetherGroupIds;

    private List<UUID> installmentPlanIds;

    private List<UUID> eventIds;

    private ExternalLinkRequest externalLink;
}