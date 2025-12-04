package org.nextgate.nextgatebackend.e_events.category.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;

    @Pattern(
            regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|svg|webp)|/icons/.*\\.(jpg|jpeg|png|gif|svg|webp))$",
            message = "Icon URL must be a valid image URL or path"
    )
    private String iconUrl;

    @Pattern(
            regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "Color code must be a valid hex color (e.g., #FF5733 or #F57)"
    )
    private String colorCode;


    @NotNull(message = "Active status is required")
    private Boolean isActive;

    @NotNull(message = "Featured status is required")
    private Boolean isFeatured;
}