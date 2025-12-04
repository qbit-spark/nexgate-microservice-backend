package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.categories.paylaod;

import lombok.Data;

import java.util.List;

@Data
public class CreateShopCategoryRequest {
    private String categoryName;
    private String categoryDescription;
    private String categoryIconUrl;

}
