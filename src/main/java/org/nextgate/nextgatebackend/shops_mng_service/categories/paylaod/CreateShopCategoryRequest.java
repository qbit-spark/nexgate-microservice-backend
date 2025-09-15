package org.nextgate.nextgatebackend.shops_mng_service.categories.paylaod;

import lombok.Data;

import java.util.List;

@Data
public class CreateShopCategoryRequest {
    private String categoryName;
    private String categoryDescription;
    private String categoryIconUrl;

}
