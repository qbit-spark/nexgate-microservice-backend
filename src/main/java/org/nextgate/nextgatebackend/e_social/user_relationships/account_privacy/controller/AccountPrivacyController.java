package org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.payloads.*;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.entity.UserPrivacySettings;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.service.AccountPrivacyService;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.utils.PrivacyMapper;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/e-social/privacy")
@RequiredArgsConstructor
public class AccountPrivacyController {

    private final AccountPrivacyService privacyService;
    private final PrivacyMapper privacyMapper;

    @GetMapping("/account")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPrivacySettings() {
        UserPrivacySettings settings = privacyService.getOrCreatePrivacySettings();
        PrivacySettingsResponse response = privacyMapper.toResponse(settings);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Privacy settings retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/account")
    public ResponseEntity<GlobeSuccessResponseBuilder> updatePrivacySettings(
            @Valid @RequestBody UpdatePrivacyRequest request) {

        UserPrivacySettings settings = privacyService.updatePrivacySettings(request.getIsPrivate());
        PrivacySettingsResponse response = privacyMapper.toResponse(settings);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Privacy settings updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }
}