package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.categories.repo.ShopCategoryRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final AccountRepo accountRepo;
    private final ShopRepo shopRepository;
    private final ShopCategoryRepo shopCategoryRepo;

    @Override
    @Transactional
    public ShopEntity createShop(CreateShopRequest request) throws ItemReadyExistException, ItemNotFoundException {

        AccountEntity owner = getAuthenticatedAccount();

        if (shopRepository.existsByShopNameAndIsDeletedFalse(request.getShopName())) {
            throw new ItemReadyExistException("Shop with this name already exists");
        }

        ShopCategoryEntity category = shopCategoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ItemNotFoundException("Shop category not found"));

        ShopEntity shop = new ShopEntity();
        shop.setShopName(request.getShopName());
        shop.setShopDescription(request.getShopDescription());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setPhoneNumber(request.getPhoneNumber());
        shop.setCity(request.getCity());
        shop.setRegion(request.getRegion());
        shop.setStatus(ShopStatus.PENDING);
        shop.setIsDeleted(false);

        System.out.println("Setting owners------->"+owner.getId());
        System.out.println("Setting category ------->"+category.getCategoryName());

        shop.setCategory(category);
        shop.setOwner(owner);

        System.out.println("Setting owners after------->"+owner.getId());
        System.out.println("Setting category after ------->"+category.getCategoryName());

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

        return shopRepository.save(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopEntity> getAllShops() {
        return shopRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopEntity> getAllShopsByStatus(ShopStatus status) {
        return shopRepository.findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopEntity> getAllFeaturedShops() {
        return shopRepository.findByIsDeletedFalseAndIsFeaturedTrueOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> getAllShopsPaged(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        return shopRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> getAllShopsByStatusPaged(ShopStatus status, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        return shopRepository.findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> getAllFeaturedShopsPaged(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        return shopRepository.findByIsDeletedFalseAndIsFeaturedTrueOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> searchShopsByName(String searchTerm, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        return shopRepository.findByIsDeletedFalseAndShopNameContainingIgnoreCaseOrderByCreatedAtDesc(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopEntity> getAllShopsSummary() {
        return shopRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> getAllShopsSummaryPaged(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        return shopRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
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