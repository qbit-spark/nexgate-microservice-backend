package org.nextgate.nextgatebackend.e_events.category.payloads;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCategoryResponse {

    private UUID categoryId;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private String colorCode;
    private Boolean isActive;
    private Boolean isFeatured;
    private Long eventCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}