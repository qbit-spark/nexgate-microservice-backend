package org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryResponse {

    private UUID categoryId;
    private String categoryName;
    private String categoryDescription;
    private String categoryIconUrl;

    private UUID parentCategoryId;
    private String parentCategoryName;

    private LocalDateTime createdTime;
    private LocalDateTime editedTime;
    private Boolean isActive;
    private UUID createdBy;
    private UUID editedBy;
}