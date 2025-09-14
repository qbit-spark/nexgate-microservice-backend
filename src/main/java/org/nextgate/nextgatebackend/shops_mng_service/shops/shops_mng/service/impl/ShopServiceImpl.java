package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.categories.repo.ShopCategoryRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.UpdateShopRequest;
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
import java.util.UUID;

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


    @Override
    @Transactional
    public ShopEntity updateShop(UUID shopId, UpdateShopRequest request) throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new RandomExceptions("Cannot update a deleted shop");
        }

        if (!shop.getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("Only shop owners can update their shops");
        }

        if (request.getCategoryId() != null) {
            ShopCategoryEntity category = shopCategoryRepo.findById(request.getCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException("Shop category not found"));
            shop.setCategory(category);
        }

        if (request.getShopName() != null) shop.setShopName(request.getShopName());
        if (request.getShopDescription() != null) shop.setShopDescription(request.getShopDescription());
        if (request.getTagline() != null) shop.setTagline(request.getTagline());
        if (request.getLogoUrl() != null) shop.setLogoUrl(request.getLogoUrl());
        if (request.getBannerUrl() != null) shop.setBannerUrl(request.getBannerUrl());
        if (request.getShopImages() != null) shop.setShopImages(request.getShopImages());
        if (request.getShopType() != null) shop.setShopType(request.getShopType());
        if (request.getPhoneNumber() != null) shop.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) shop.setEmail(request.getEmail());
        if (request.getWebsiteUrl() != null) shop.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getSocialMediaLinks() != null) shop.setSocialMediaLinks(request.getSocialMediaLinks());
        if (request.getAddress() != null) shop.setAddress(request.getAddress());
        if (request.getCity() != null) shop.setCity(request.getCity());
        if (request.getRegion() != null) shop.setRegion(request.getRegion());
        if (request.getPostalCode() != null) shop.setPostalCode(request.getPostalCode());
        if (request.getCountryCode() != null) shop.setCountryCode(request.getCountryCode());
        if (request.getLatitude() != null) shop.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) shop.setLongitude(request.getLongitude());
        if (request.getLocationNotes() != null) shop.setLocationNotes(request.getLocationNotes());
        if (request.getPromotionText() != null) shop.setPromotionText(request.getPromotionText());

        return shopRepository.save(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopEntity getShopById(UUID shopId) throws ItemNotFoundException {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        return shop;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopEntity> getShopsByCategory(UUID categoryId) throws ItemNotFoundException {
        if (!shopCategoryRepo.existsById(categoryId)) {
            throw new ItemNotFoundException("Category not found");
        }

        return shopRepository.findByCategoryCategoryIdAndIsDeletedFalseOrderByCreatedAtDesc(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> getShopsByCategoryPaged(UUID categoryId, int page, int size) throws ItemNotFoundException {
        if (!shopCategoryRepo.existsById(categoryId)) {
            throw new ItemNotFoundException("Category not found");
        }

        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size);
        return shopRepository.findByCategoryCategoryIdAndIsDeletedFalseOrderByCreatedAtDesc(categoryId, pageable);
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