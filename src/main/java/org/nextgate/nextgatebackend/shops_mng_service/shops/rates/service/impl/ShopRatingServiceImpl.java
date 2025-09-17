package org.nextgate.nextgatebackend.shops_mng_service.shops.rates.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.entity.ShopRatingEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.CreateRatingRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.ShopRatingSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.UpdateRatingRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.repo.ShopRatingRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.service.ShopRatingService;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopRatingServiceImpl implements ShopRatingService {

    private final ShopRatingRepo shopRatingRepo;
    private final ShopRepo shopRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public ShopRatingEntity createRating(UUID shopId, CreateRatingRequest request) throws ItemNotFoundException, ItemReadyExistException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new RandomExceptions("Cannot rate a deleted shop");
        }

        if (shop.getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("Shop owners cannot rate their own shops");
        }

        if (shopRatingRepo.existsByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId())) {
            throw new ItemReadyExistException("You have already rated this shop. Use update to change your rating.");
        }

        ShopRatingEntity rating = new ShopRatingEntity();
        rating.setShop(shop);
        rating.setUser(user);
        rating.setRatingValue(request.getRatingValue());
        rating.setIsDeleted(false);

        return shopRatingRepo.save(rating);
    }

    @Override
    @Transactional
    public ShopRatingEntity updateRating(UUID shopId, UpdateRatingRequest request) throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ShopRatingEntity existingRating = shopRatingRepo.findByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId())
                .orElseThrow(() -> new ItemNotFoundException("Rating not found. Create a rating first."));

        existingRating.setRatingValue(request.getRatingValue());
        existingRating.setUpdatedAt(LocalDateTime.now());

        return shopRatingRepo.save(existingRating);
    }

    @Override
    @Transactional
    public void deleteRating(UUID shopId) throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ShopRatingEntity existingRating = shopRatingRepo.findByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId())
                .orElseThrow(() -> new ItemNotFoundException("Rating not found"));

        existingRating.setIsDeleted(true);
        existingRating.setUpdatedAt(LocalDateTime.now());

        shopRatingRepo.save(existingRating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopRatingEntity> getRatingsByShop(UUID shopId) throws ItemNotFoundException {

        if (!shopRepo.existsById(shopId)) {
            throw new ItemNotFoundException("Shop not found");
        }

        return shopRatingRepo.findByShopShopIdAndIsDeletedFalseOrderByCreatedAtDesc(shopId);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopRatingEntity getUserRatingForShop(UUID shopId) throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();

        return shopRatingRepo.findByShopShopIdAndUserIdAndIsDeletedFalse(shopId, user.getId())
                .orElseThrow(() -> new ItemNotFoundException("You have not rated this shop yet"));
    }

    @Override
    @Transactional(readOnly = true)
    public ShopRatingSummaryResponse getShopRatingSummary(UUID shopId) throws ItemNotFoundException {

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        List<ShopRatingEntity> allRatings = shopRatingRepo.findByShopShopIdAndIsDeletedFalse(shopId);

        long totalRatings = allRatings.size();
        double averageRating = 0.0;
        Map<Integer, Long> ratingDistribution = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }

        if (totalRatings > 0) {
            int sum = 0;
            for (ShopRatingEntity rating : allRatings) {
                sum += rating.getRatingValue();

                Integer ratingValue = rating.getRatingValue();
                ratingDistribution.put(ratingValue, ratingDistribution.get(ratingValue) + 1);
            }
            averageRating = Math.round((double) sum / totalRatings * 10.0) / 10.0;
        }

        return ShopRatingSummaryResponse.builder()
                .shopId(shopId)
                .shopName(shop.getShopName())
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .ratingDistribution(ratingDistribution)
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