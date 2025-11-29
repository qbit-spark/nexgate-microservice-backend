package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.impl;


import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.SessionMetadataExtractor;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSessionMetadataExtractor implements SessionMetadataExtractor {

    private final ShopRepo shopRepo;

    @Override
    public AccountEntity extractPayee(PayableCheckoutSession session) throws RandomExceptions {

        if (!(session instanceof ProductCheckoutSessionEntity productSession)) {
            throw new RandomExceptions("Invalid session type for ProductSessionMetadataExtractor");
        }

        if (productSession.getItems() == null || productSession.getItems().isEmpty()) {
            throw new RandomExceptions("No items in product checkout session");
        }

        UUID shopId = productSession.getItems().getFirst().getShopId();

        var shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new RandomExceptions("Shop not found: " + shopId));

        AccountEntity payee = shop.getOwner();

        log.debug("Payee extracted: {} (Shop: {})", payee.getUserName(), shop.getShopName());

        return payee;
    }

    @Override
    public CheckoutSessionsDomains getSupportedDomain() {
        return CheckoutSessionsDomains.PRODUCT;
    }
}