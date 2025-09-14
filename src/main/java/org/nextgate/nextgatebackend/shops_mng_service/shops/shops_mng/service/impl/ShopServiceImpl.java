package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.impl;

import kotlin.jvm.internal.SerializedIr;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.categories.repo.ShopCategoryRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.ShopService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final AccountRepo accountRepo;
    private final ShopRepo shopRepository;
    private final ShopCategoryRepo shopCategoryRepo;

    @Override
    public GlobeSuccessResponseBuilder createShop(CreateShopRequest request) throws ItemReadyExistException, ItemNotFoundException {

        // Get the current user (owner)
        AccountEntity owner = getAuthenticatedAccount();

        // Check if the shop name already exists
        if (shopRepository.existsByShopNameAndIsDeletedFalse(request.getShopName())) {
            throw new ItemReadyExistException("Shop with this name already exists");
        }

        // Check if a category exists
        ShopCategoryEntity category = shopCategoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ItemNotFoundException("Shop category not found"));

        // Create a new shop
        ShopEntity shop = new ShopEntity();
        shop.setShopName(request.getShopName());
        shop.setShopDescription(request.getShopDescription());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setPhoneNumber(request.getPhoneNumber());
        shop.setCity(request.getCity());
        shop.setRegion(request.getRegion());
        shop.setStatus(ShopStatus.PENDING);
        shop.setIsDeleted(false);
        shop.setCategory(category);

        // Set optional fields
        shop.setTagline(request.getTagline());
        shop.setShopImages(request.getShopImages());
        shop.setBannerUrl(request.getBannerUrl());
        shop.setShopType(request.getShopType());
        shop.setEmail(request.getEmail());
        shop.setWebsiteUrl(request.getWebsiteUrl());
        shop.setSocialMediaLinks(request.getSocialMediaLinks());
        shop.setAddress(request.getAddress());
        shop.setPostalCode(request.getPostalCode());
        shop.setCountryCode(request.getCountryCode());
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());
        shop.setLocationNotes(request.getLocationNotes());
        shop.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        shop.setTaxNumber(request.getTaxNumber());
        shop.setLicenseNumber(request.getLicenseNumber());
        shop.setPromotionText(request.getPromotionText());

        ShopEntity savedShop = shopRepository.save(shop);

        return GlobeSuccessResponseBuilder.success(
                "Shop created successfully and is pending approval",
                savedShop
        );
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}
