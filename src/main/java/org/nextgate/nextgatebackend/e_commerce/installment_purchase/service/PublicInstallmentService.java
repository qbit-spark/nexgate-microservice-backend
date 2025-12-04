package org.nextgate.nextgatebackend.e_commerce.installment_purchase.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.InstallmentPlanResponse;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.InstallmentPreviewRequest;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.InstallmentPreviewResponse;

import java.util.List;
import java.util.UUID;


public interface PublicInstallmentService {
    List<InstallmentPlanResponse> getAvailablePlans(UUID productId) throws ItemNotFoundException;

    public InstallmentPreviewResponse calculatePreview(InstallmentPreviewRequest request) throws ItemNotFoundException, BadRequestException;

}