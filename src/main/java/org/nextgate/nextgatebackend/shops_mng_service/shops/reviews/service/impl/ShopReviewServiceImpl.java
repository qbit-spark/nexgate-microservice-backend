package org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.entity.ShopReviewEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.enums.ReviewStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.CreateReviewRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.ShopReviewSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.UpdateReviewRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.repo.ShopReviewRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.service.ShopReviewService;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopReviewServiceImpl implements ShopReviewService {

    private final ShopReviewRepo shopReviewRepo;
    private final ShopRepo shopRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public ShopReviewEntity createReview(UUID shopId, CreateReviewRequest request) throws ItemNotFoundException, ItemReadyExistException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new RandomExceptions("Cannot review a deleted shop");
        }

        if (shop.getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("Shop owners cannot review their own shops");
        }

        if (shopReviewRepo.existsByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId())) {
            throw new ItemReadyExistException("You have already reviewed this shop. Use update to change your review.");
        }

        ShopReviewEntity review = new ShopReviewEntity();
        review.setShop(shop);
        review.setUser(user);
        review.setReviewText(request.getReviewText());
        review.setStatus(ReviewStatus.ACTIVE);
        review.setIsDeleted(false);

        return shopReviewRepo.save(review);
    }

    @Override
    @Transactional
    public ShopReviewEntity updateReview(UUID shopId, UpdateReviewRequest request) throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ShopReviewEntity existingReview = shopReviewRepo.findByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId())
                .orElseThrow(() -> new ItemNotFoundException("Review not found. Create a review first."));

        existingReview.setReviewText(request.getReviewText());
        existingReview.setUpdatedAt(LocalDateTime.now());

        return shopReviewRepo.save(existingReview);
    }

    @Override
    @Transactional
    public void deleteReview(UUID shopId) throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ShopReviewEntity existingReview = shopReviewRepo.findByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId())
                .orElseThrow(() -> new ItemNotFoundException("Review not found"));

        existingReview.setIsDeleted(true);
        existingReview.setUpdatedAt(LocalDateTime.now());

        shopReviewRepo.save(existingReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopReviewEntity> getActiveReviewsByShop(UUID shopId) throws ItemNotFoundException {

        if (!shopRepo.existsById(shopId)) {
            throw new ItemNotFoundException("Shop not found");
        }

        return shopReviewRepo.findByShopShopIdAndIsDeletedFalseAndStatusOrderByCreatedAtDesc(shopId, ReviewStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopReviewEntity> getActiveReviewsByShopPaged(UUID shopId, int page, int size) throws ItemNotFoundException {

        if (!shopRepo.existsById(shopId)) {
            throw new ItemNotFoundException("Shop not found");
        }

        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size); // Subtract 1 here

        return shopReviewRepo.findByShopShopIdAndIsDeletedFalseAndStatusOrderByCreatedAtDesc(shopId, ReviewStatus.ACTIVE, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopReviewEntity getUserReviewForShop(UUID shopId) throws ItemNotFoundException {
        AccountEntity user = getAuthenticatedAccount();
        return shopReviewRepo.findByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopReviewSummaryResponse getShopReviewSummary(UUID shopId) throws ItemNotFoundException {

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        long activeReviews = shopReviewRepo.countByShopShopIdAndIsDeletedFalseAndStatus(shopId, ReviewStatus.ACTIVE);
        long hiddenReviews = shopReviewRepo.countByShopShopIdAndIsDeletedFalseAndStatus(shopId, ReviewStatus.HIDDEN);
        long flaggedReviews = shopReviewRepo.countByShopShopIdAndIsDeletedFalseAndStatus(shopId, ReviewStatus.FLAGGED);
        long totalReviews = activeReviews + hiddenReviews + flaggedReviews;

        return ShopReviewSummaryResponse.builder()
                .shopId(shopId)
                .shopName(shop.getShopName())
                .totalReviews(totalReviews)
                .activeReviews(activeReviews)
                .hiddenReviews(hiddenReviews)
                .flaggedReviews(flaggedReviews)
                .build();
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