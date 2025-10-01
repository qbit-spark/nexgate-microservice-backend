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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
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
        shop.setStatus(ShopStatus.ACTIVE);
        shop.setIsDeleted(false);
        shop.setCategory(category);
        shop.setOwner(owner);
        shop.setTagline(request.getTagline());
        shop.setShopImages(request.getShopImages());
        shop.setBannerUrl(request.getBannerUrl());
        shop.setShopType(request.getShopType());
        shop.setEmail(request.getEmail());
        shop.setApproved(true);
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
        String uniqueSlug = generateUniqueShopSlug(request.getShopName());
        shop.setShopSlug(uniqueSlug);

        return shopRepository.save(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopEntity> getAllShops() {
        return shopRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> getAllShopsPaged(int page, int size) {
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size);
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

        // Update shop name and regenerate slug if name changed
        if (request.getShopName() != null) {
            // Check if name actually changed
            if (!shop.getShopName().equals(request.getShopName())) {
                shop.setShopName(request.getShopName());

                // Regenerate slug only if name changed
                String newSlug = generateUniqueShopSlug(request.getShopName());
                shop.setShopSlug(newSlug);
            }
        }

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
    public ShopEntity getShopByIdDetailed(UUID shopId) throws ItemNotFoundException, RandomExceptions {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        AccountEntity user = getAuthenticatedAccount();
        if (!shop.getOwner().getId().equals(user.getId()) && !(user.getRoles().contains("ROLE_SUPER_ADMIN") || user.getRoles().contains("ROLE_STAFF_ADMIN"))) {
            throw new RandomExceptions("Only shop owners and administrators can access detailed shop information");
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

        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size); // Subtract 1 here

        return shopRepository.findByCategoryCategoryIdAndIsDeletedFalseOrderByCreatedAtDesc(categoryId, pageable);

    }

    @Override
    public ShopEntity approveShop(UUID shopId, boolean approve) throws ItemNotFoundException {

        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        shop.setApproved(approve);
        shop.setApprovedByUser(getAuthenticatedAccount());

        return shopRepository.save(shop);
    }

    @Override
    public List<ShopEntity> getMyShops() throws ItemNotFoundException {
        return shopRepository.findByOwner(getAuthenticatedAccount());
    }

    @Override
    public Page<ShopEntity> getMyShopsPaged(int page, int size) throws ItemNotFoundException {
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size); // Subtract 1 here
        return shopRepository.findByOwner(getAuthenticatedAccount(), pageable);

    }


    @Override
    @Transactional(readOnly = true)
    public List<ShopEntity> getFeaturedShops() {
        List<ShopEntity> allShops = shopRepository.findByIsDeletedFalseAndIsApprovedTrueOrderByCreatedAtDesc();

        // Shuffle the list to randomize
        List<ShopEntity> shuffledShops = new ArrayList<>(allShops);
        Collections.shuffle(shuffledShops);

        // Return up to 20 random shops
        return shuffledShops.stream()
                .limit(20)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopEntity> getFeaturedShopsPaged(int page, int size) {
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        // Get all shops and shuffle them
        List<ShopEntity> allShops = shopRepository.findByIsDeletedFalseAndIsApprovedTrueOrderByCreatedAtDesc();
        List<ShopEntity> shuffledShops = new ArrayList<>(allShops);
        Collections.shuffle(shuffledShops);

        // Manual pagination on the shuffled list
        int start = (page - 1) * size;
        int end = Math.min(start + size, shuffledShops.size());

        List<ShopEntity> pageContent = start < shuffledShops.size() ?
                shuffledShops.subList(start, end) : new ArrayList<>();

        // Create a Page object manually
        return new PageImpl<>(pageContent, PageRequest.of(page - 1, size), shuffledShops.size());
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

    // Add these helper methods at the bottom of ShopServiceImpl class:

    private String generateUniqueShopSlug(String shopName) {
        String baseSlug = createBaseSlug(shopName);
        String slug = baseSlug;
        int counter = 2;

        while (shopRepository.existsByShopSlugAndIsDeletedFalse(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private String createBaseSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "shop";
        }

        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}