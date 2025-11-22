package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.payloads.GroupParticipantResponse;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.payloads.GroupPurchaseResponse;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.payloads.GroupPurchaseSummaryResponse;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.payloads.TransferGroupRequest;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.service.GroupPurchaseService;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.utils.mappers.GroupPurchaseMapper;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/group-purchases")
@RequiredArgsConstructor
@Slf4j
public class GroupPurchaseController {

    private final GroupPurchaseService groupPurchaseService;
    private final GroupPurchaseMapper groupPurchaseMapper;
    private final ProductRepo productRepo;
    private final AccountRepo accountRepo;

    @GetMapping("/product/{productId}/available")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAvailableGroupsForProduct(
            @PathVariable UUID productId
    ) throws ItemNotFoundException {

        log.info("Getting available groups for product: {}", productId);

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        List<GroupPurchaseInstanceEntity> groups =
                groupPurchaseService.getAvailableGroupsForProduct(product);

        List<GroupPurchaseSummaryResponse> responses =
                groupPurchaseMapper.toSummaryResponseList(groups, authenticatedUser);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Available groups retrieved successfully",
                        responses
                )
        );
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getGroupById(
            @PathVariable UUID groupId
    ) throws ItemNotFoundException {

        log.info("Getting group by ID: {}", groupId);

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        GroupPurchaseInstanceEntity group =
                groupPurchaseService.getGroupById(groupId);

        GroupPurchaseResponse response =
                groupPurchaseMapper.toFullResponse(group, authenticatedUser);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Group retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/code/{groupCode}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getGroupByCode(
            @PathVariable String groupCode
    ) throws ItemNotFoundException {

        log.info("Getting group by code: {}", groupCode);

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        GroupPurchaseInstanceEntity group =
                groupPurchaseService.getGroupByCode(groupCode);

        GroupPurchaseResponse response =
                groupPurchaseMapper.toFullResponse(group, authenticatedUser);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Group retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/my-groups")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyGroups(
            @RequestParam(required = false) GroupStatus status
    ) throws ItemNotFoundException {

        log.info("Getting my groups with status: {}", status);

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        List<GroupPurchaseInstanceEntity> groups =
                groupPurchaseService.getGroupsUserBelongsTo(authenticatedUser, status);

        List<GroupPurchaseSummaryResponse> responses =
                groupPurchaseMapper.toSummaryResponseList(groups, authenticatedUser);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "My groups retrieved successfully",
                        responses
                )
        );
    }

    @GetMapping("/my-participations")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyParticipations()
            throws ItemNotFoundException {

        log.info("Getting my participations");

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        List<GroupParticipantEntity> participations =
                groupPurchaseService.getMyActiveParticipations(authenticatedUser);

        List<GroupParticipantResponse> responses =
                groupPurchaseMapper.toParticipantResponseList(participations, authenticatedUser);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "My participations retrieved successfully",
                        responses
                )
        );
    }

    @PostMapping("/transfer")
    public ResponseEntity<GlobeSuccessResponseBuilder> transferSeats(
            @Valid @RequestBody TransferGroupRequest request
    ) throws ItemNotFoundException, BadRequestException {

        log.info("Transferring {} seats from {} to {}",
                request.getQuantity(),
                request.getSourceGroupId(),
                request.getTargetGroupId());

        GroupParticipantEntity updatedParticipant =
                groupPurchaseService.transferToGroup(
                        request.getSourceGroupId(),
                        request.getTargetGroupId(),
                        request.getQuantity()
                );

        GroupParticipantResponse response =
                groupPurchaseMapper.toParticipantResponse(updatedParticipant, true);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Seats transferred successfully",
                        response
                )
        );
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