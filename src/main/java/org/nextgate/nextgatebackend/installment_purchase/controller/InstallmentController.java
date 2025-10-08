package org.nextgate.nextgatebackend.installment_purchase.controller;

//@RestController
//@RequestMapping("/api/v1/installments")
public class InstallmentController {
//
//    // Get my agreements
//    @GetMapping("/my-agreements")
//    public ResponseEntity<List<InstallmentAgreementSummaryResponse>>
//    getMyAgreements(
//            @RequestParam(required = false) AgreementStatus status
//    );
//
//    // Get active agreements
//    @GetMapping("/my-agreements/active")
//    public ResponseEntity<List<InstallmentAgreementSummaryResponse>>
//    getMyActiveAgreements();
//
//    // Get agreement details
//    @GetMapping("/agreements/{agreementId}")
//    public ResponseEntity<InstallmentAgreementResponse>
//    getAgreementById(@PathVariable UUID agreementId);
//
//    // Get agreement by number
//    @GetMapping("/agreements/number/{agreementNumber}")
//    public ResponseEntity<InstallmentAgreementResponse>
//    getAgreementByNumber(@PathVariable String agreementNumber);
//
//    // Get payments for agreement
//    @GetMapping("/agreements/{agreementId}/payments")
//    public ResponseEntity<List<InstallmentPaymentResponse>>
//    getAgreementPayments(@PathVariable UUID agreementId);
//
//    // Get upcoming payments
//    @GetMapping("/upcoming-payments")
//    public ResponseEntity<List<InstallmentPaymentResponse>>
//    getUpcomingPayments();
//
//    // Make manual payment
//    @PostMapping("/agreements/{agreementId}/payments/{paymentId}/pay")
//    public ResponseEntity<PaymentResponse> makeManualPayment(
//            @PathVariable UUID agreementId,
//            @PathVariable UUID paymentId,
//            @RequestBody MakePaymentRequest request
//    );
//
//    // Retry failed payment
//    @PostMapping("/payments/{paymentId}/retry")
//    public ResponseEntity<PaymentResponse> retryPayment(
//            @PathVariable UUID paymentId
//    );
//
//    // Calculate early payoff
//    @GetMapping("/agreements/{agreementId}/early-payoff")
//    public ResponseEntity<EarlyPayoffCalculation> calculateEarlyPayoff(
//            @PathVariable UUID agreementId
//    );
//
//    // Process early payoff
//    @PostMapping("/agreements/{agreementId}/early-payoff")
//    public ResponseEntity<PaymentResponse> processEarlyPayoff(
//            @PathVariable UUID agreementId,
//            @RequestBody EarlyPayoffRequest request
//    );
//
//    // Cancel agreement
//    @PostMapping("/agreements/{agreementId}/cancel")
//    public ResponseEntity<Void> cancelAgreement(
//            @PathVariable UUID agreementId,
//            @RequestBody CancelAgreementRequest request
//    );
}