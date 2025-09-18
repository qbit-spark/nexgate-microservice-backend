package org.nextgate.nextgatebackend.products_mng_service.categories.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

@Data
public class CreateProductCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String categoryName;

    @Size(max = 500, message = "Category description must not exceed 500 characters")
    private String categoryDescription;

    @URL(message = "Category icon URL must be valid")
    @Size(max = 1000, message = "Category icon URL must not exceed 1000 characters")
    private String categoryIconUrl;

    private UUID parentCategoryId; // For hierarchical categories
}