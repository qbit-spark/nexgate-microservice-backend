package org.nextgate.nextgatebackend.shops_mng_service.categories.paylaod;

import lombok.Data;

@Data
public class CreateShopCategoryRequest {
    private String categoryName;
    private String categoryDescription;
}
