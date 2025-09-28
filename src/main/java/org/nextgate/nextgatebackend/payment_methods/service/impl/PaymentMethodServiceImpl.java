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
import org.nextgate.nextgatebackend.payment_methods.payload.PaymentMethodSummaryResponse;
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
    public PaymentMethodDetailResponse getPaymentMethodById(UUID paymentMethodId) throws ItemNotFoundException {
        // Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Find payment method and verify ownership
        PaymentMethodsEntity paymentMethod = paymentMethodRepository
                .findByPaymentMethodIdAndOwner(paymentMethodId, authenticatedAccount)
                .orElseThrow(() -> new ItemNotFoundException("Payment method not found, or you do not have access to it"));

        // Convert to response DTO
        return paymentMethodMapper.toDetailResponse(paymentMethod);
    }

    @Override
    public PaymentMethodListResponse getMyPaymentMethods() throws ItemNotFoundException {
        // Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Get all payment methods for the user
        List<PaymentMethodsEntity> paymentMethods = paymentMethodRepository.findByOwner(authenticatedAccount);

        // Convert to summary responses
        List<PaymentMethodSummaryResponse> summaryResponses = paymentMethodMapper.toSummaryResponseList(paymentMethods);

        // Find default payment method
        PaymentMethodSummaryResponse defaultMethod = summaryResponses.stream()
                .filter(PaymentMethodSummaryResponse::getIsDefault)
                .findFirst()
                .orElse(null);

        // Calculate counts
        int totalCount = paymentMethods.size();
        int activeCount = (int) paymentMethods.stream()
                .filter(PaymentMethodsEntity::getIsActive)
                .count();

        return PaymentMethodListResponse.builder()
                .paymentMethods(summaryResponses)
                .totalCount(totalCount)
                .activeCount(activeCount)
                .defaultPaymentMethod(defaultMethod)
                .build();
    }

    @Override
    @Transactional
    public PaymentMethodDetailResponse updatePaymentMethod(UUID paymentMethodId, CreatePaymentMethodRequest request) throws ItemNotFoundException, BadRequestException {
        // Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Find payment method and verify ownership
        PaymentMethodsEntity existingPaymentMethod = paymentMethodRepository
                .findByPaymentMethodIdAndOwner(paymentMethodId, authenticatedAccount)
                .orElseThrow(() -> new ItemNotFoundException("Payment method not found"));

        // Validate payment method details (only validate provided fields)
        paymentMethodValidator.validatePaymentMethodDetails(request);

        // Check for duplicates (excluding current payment method)
        paymentMethodValidator.checkForDuplicateOnUpdate(authenticatedAccount, request, paymentMethodId);

        // Handle default payment method logic
        if (request.getIsDefault() != null && request.getIsDefault() && !existingPaymentMethod.getIsDefault()) {
            paymentMethodHelper.unsetCurrentDefaultPaymentMethod(authenticatedAccount);
        }

        // Partial update - only update provided fields
        paymentMethodHelper.updatePaymentMethodFields(existingPaymentMethod, request);

        // Save updated entity
        PaymentMethodsEntity savedEntity = paymentMethodRepository.save(existingPaymentMethod);

        // Convert to response DTO
        return paymentMethodMapper.toDetailResponse(savedEntity);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(UUID paymentMethodId) throws ItemNotFoundException {
        // Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Find payment method and verify ownership
        PaymentMethodsEntity paymentMethod = paymentMethodRepository
                .findByPaymentMethodIdAndOwner(paymentMethodId, authenticatedAccount)
                .orElseThrow(() -> new ItemNotFoundException("Payment method not found"));

        // Hard delete - permanently remove from database
        paymentMethodRepository.delete(paymentMethod);
    }

    @Override
    @Transactional
    public PaymentMethodDetailResponse setAsDefault(UUID paymentMethodId) throws ItemNotFoundException, BadRequestException {
        // Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Find payment method and verify ownership
        PaymentMethodsEntity paymentMethod = paymentMethodRepository
                .findByPaymentMethodIdAndOwner(paymentMethodId, authenticatedAccount)
                .orElseThrow(() -> new ItemNotFoundException("Payment method not found"));

        // Check if payment method is active
        if (!paymentMethod.getIsActive()) {
            throw new BadRequestException("Cannot set inactive payment method as default");
        }

        // Unset current default payment methods
        paymentMethodHelper.unsetCurrentDefaultPaymentMethod(authenticatedAccount);

        // Set this payment method as default
        paymentMethod.setIsDefault(true);
        PaymentMethodsEntity savedEntity = paymentMethodRepository.save(paymentMethod);

        // Convert to response DTO
        return paymentMethodMapper.toDetailResponse(savedEntity);
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
