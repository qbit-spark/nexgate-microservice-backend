package org.nextgate.nextgatebackend.e_events.category.utils.mappers;

import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.category.payloads.EventCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class EventCategoryMapper {

    public EventCategoryResponse toResponse(EventsCategoryEntity entity) {
        return EventCategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .iconUrl(entity.getIconUrl())
                .colorCode(entity.getColorCode())
                .isActive(entity.getIsActive())
                .isFeatured(entity.getIsFeatured())
                .eventCount(entity.getEventCount())
                .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getUserName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getUserName() : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}