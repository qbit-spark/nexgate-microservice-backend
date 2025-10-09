package org.nextgate.nextgatebackend.installment_purchase.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.installment_purchase.payloads.InstallmentPlanResponse;
import org.nextgate.nextgatebackend.installment_purchase.payloads.InstallmentPreviewRequest;
import org.nextgate.nextgatebackend.installment_purchase.payloads.InstallmentPreviewResponse;
import org.nextgate.nextgatebackend.installment_purchase.service.PublicInstallmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/installments")
@RequiredArgsConstructor
@Slf4j
public class PublicInstallmentController {

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
}