package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;

@Data
@Builder
@Getter
public class SearchContext {
    private ShopEntity shop;
    private AccountEntity user;
    private boolean isPublicUser;
    private boolean isShopOwner;
    private boolean isSystemAdmin;
    private boolean canSearchAllStatuses;
}
