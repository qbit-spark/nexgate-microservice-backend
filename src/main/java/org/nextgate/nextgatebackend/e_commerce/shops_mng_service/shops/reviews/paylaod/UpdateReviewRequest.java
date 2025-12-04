package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.paylaod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateReviewRequest {

    @NotBlank(message = "Review text is required")
    @Size(min = 10, max = 1000, message = "Review must be between 10 and 1000 characters")
    private String reviewText;
}
