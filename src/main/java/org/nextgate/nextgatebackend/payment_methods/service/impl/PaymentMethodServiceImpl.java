package org.nextgate.nextgatebackend.payment_methods.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.payload.CreatePaymentMethodRequest;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodDetailResponse;
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodListResponse;
import org.nextgate.nextgatebackend.payment_methods.repo.PaymentMethodRepository;
import org.nextgate.nextgatebackend.payment_methods.service.PaymentMethodService;
import org.nextgate.nextgatebackend.payment_methods.utils.validators.PaymentMethodHelper;
import org.nextgate.nextgatebackend.payment_methods.utils.validators.PaymentMethodMapper;
import org.nextgate.nextgatebackend.payment_methods.utils.validators.PaymentMethodValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final AccountRepo accountRepo;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodValidator paymentMethodValidator;
    private final PaymentMethodHelper paymentMethodHelper;
    private final PaymentMethodMapper paymentMethodMapper;


    @Override
    @Transactional
    public PaymentMethodDetailResponse createPaymentMethod(CreatePaymentMethodRequest request) throws ItemNotFoundException, BadRequestException {

        // Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Validate payment method details based on type
        paymentMethodValidator.validatePaymentMethodDetails(request);

        // Check if payment method already exists for this user
        paymentMethodValidator.checkForDuplicatePaymentMethod(authenticatedAccount, request);

        // Handle default payment method logic
        if (request.getIsDefault() != null && request.getIsDefault()) {
            paymentMethodHelper.unsetCurrentDefaultPaymentMethod(authenticatedAccount);
        }

        // Create and save payment method entity
        PaymentMethodsEntity paymentMethodEntity = paymentMethodMapper.toEntity(request, authenticatedAccount);
        PaymentMethodsEntity savedEntity = paymentMethodRepository.save(paymentMethodEntity);

        // Convert to response DTO
        return paymentMethodMapper.toDetailResponse(savedEntity);
    }

    @Override
    public PaymentMethodDetailResponse getPaymentMethodById(UUID paymentMethodId) {
        return null;
    }

    @Override
    public PaymentMethodListResponse getMyPaymentMethods() {
        return null;
    }

    @Override
    public PaymentMethodDetailResponse updatePaymentMethod(UUID paymentMethodId, CreatePaymentMethodRequest request) {
        return null;
    }

    @Override
    public void deletePaymentMethod(UUID paymentMethodId) {

    }

    @Override
    public PaymentMethodDetailResponse setAsDefault(UUID paymentMethodId) {
        return null;
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

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account, PaymentMethodsEntity paymentMethodsEntity) {
        // Check if the user has any of the custom roles
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        // Check if the user is the owner of the shop
        boolean isOwner = paymentMethodsEntity.getOwner().getAccountId().equals(account.getAccountId());

        return hasCustomRole || isOwner;
    }

}
