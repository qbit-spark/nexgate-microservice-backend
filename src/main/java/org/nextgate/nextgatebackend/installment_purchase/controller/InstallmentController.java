package org.nextgate.nextgatebackend.installment_purchase.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.installment_purchase.payloads.*;
import org.nextgate.nextgatebackend.installment_purchase.service.CustomerInstallmentService;
import org.nextgate.nextgatebackend.installment_purchase.service.PublicInstallmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/installments")
@RequiredArgsConstructor
@Slf4j
public class InstallmentController {

    private final CustomerInstallmentService customerInstallmentService;
    private final AccountRepo accountRepo;
    private final PublicInstallmentService publicInstallmentService;

    @GetMapping("/products/{productId}/plans")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAvailablePlans(
            @PathVariable UUID productId
    ) throws ItemNotFoundException {

        List<InstallmentPlanResponse> plans = publicInstallmentService
                .getAvailablePlans(productId);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Available installment plans retrieved successfully")
                .data(plans)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate-preview")
    public ResponseEntity<GlobeSuccessResponseBuilder> calculatePreview(
            @Valid @RequestBody InstallmentPreviewRequest request) throws ItemNotFoundException, BadRequestException {

        InstallmentPreviewResponse preview = publicInstallmentService.calculatePreview(request);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Installment preview calculated successfully")
                .data(preview)
                .build();

        return ResponseEntity.ok(response);
    }


    @GetMapping("/my-agreements")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyAgreements(
            @RequestParam(required = false) AgreementStatus status
    ) throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        List<InstallmentAgreementSummaryResponse> agreements =
                customerInstallmentService.getMyAgreements(customer, status);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Agreements retrieved successfully")
                .data(agreements)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-agreements/active")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyActiveAgreements()
            throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        List<InstallmentAgreementSummaryResponse> agreements =
                customerInstallmentService.getMyActiveAgreements(customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Active agreements retrieved successfully")
                .data(agreements)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/agreements/{agreementId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAgreementById(
            @PathVariable UUID agreementId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        InstallmentAgreementResponse agreement =
                customerInstallmentService.getAgreementById(agreementId, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Agreement details retrieved successfully")
                .data(agreement)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/agreements/number/{agreementNumber}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAgreementByNumber(
            @PathVariable String agreementNumber
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        InstallmentAgreementResponse agreement =
                customerInstallmentService.getAgreementByNumber(agreementNumber, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Agreement details retrieved successfully")
                .data(agreement)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/agreements/{agreementId}/payments")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAgreementPayments(
            @PathVariable UUID agreementId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        List<InstallmentPaymentDetailResponse> payments =
                customerInstallmentService.getAgreementPayments(agreementId, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Payment history retrieved successfully")
                .data(payments)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming-payments")
    public ResponseEntity<GlobeSuccessResponseBuilder> getUpcomingPayments()
            throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        List<InstallmentPaymentDetailResponse> payments =
                customerInstallmentService.getUpcomingPayments(customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Upcoming payments retrieved successfully")
                .data(payments)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/agreements/{agreementId}/payments/{paymentId}/pay")
    public ResponseEntity<GlobeSuccessResponseBuilder> makeManualPayment(
            @PathVariable UUID agreementId,
            @PathVariable UUID paymentId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        ProcessPaymentResponse payment = customerInstallmentService.makeManualPayment(
                agreementId, paymentId, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Payment processed successfully")
                .data(payment)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{paymentId}/retry")
    public ResponseEntity<GlobeSuccessResponseBuilder> retryPayment(
            @PathVariable UUID paymentId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        ProcessPaymentResponse payment = customerInstallmentService.retryPayment(
                paymentId, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Payment retry processed successfully")
                .data(payment)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/agreements/{agreementId}/early-payoff")
    public ResponseEntity<GlobeSuccessResponseBuilder> calculateEarlyPayoff(
            @PathVariable UUID agreementId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        EarlyPayoffCalculation calculation = customerInstallmentService.calculateEarlyPayoff(
                agreementId, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Early payoff calculation completed")
                .data(calculation)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/agreements/{agreementId}/early-payoff")
    public ResponseEntity<GlobeSuccessResponseBuilder> processEarlyPayoff(
            @PathVariable UUID agreementId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        ProcessPaymentResponse payment = customerInstallmentService.processEarlyPayoff(
                agreementId, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Early payoff processed successfully")
                .data(payment)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/agreements/{agreementId}/cancel")
    public ResponseEntity<GlobeSuccessResponseBuilder> cancelAgreement(
            @PathVariable UUID agreementId,
            @Valid @RequestBody CancelAgreementRequest request
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        customerInstallmentService.cancelAgreement(agreementId, request, customer);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Agreement cancelled successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/agreements/{agreementId}/early-flexible-payment/preview")
    public ResponseEntity<GlobeSuccessResponseBuilder> previewFlexiblePayment(
            @PathVariable UUID agreementId,
            @Valid @RequestBody FlexiblePaymentPreviewRequest request
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        FlexiblePaymentPreviewResponse preview =
                customerInstallmentService.previewFlexiblePayment(
                        agreementId, request, customer
                );

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message("Flexible payment preview calculated successfully")
                .data(preview)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/agreements/{agreementId}/early-flexible-payment")
    public ResponseEntity<GlobeSuccessResponseBuilder> makeFlexiblePayment(
            @PathVariable UUID agreementId,
            @Valid @RequestBody FlexiblePaymentRequest request
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        FlexiblePaymentResponse payment =
                customerInstallmentService.makeFlexiblePayment(
                        agreementId, request, customer
                );

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.builder()
                .message(payment.getMessage())
                .data(payment)
                .build();

        return ResponseEntity.ok(response);
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