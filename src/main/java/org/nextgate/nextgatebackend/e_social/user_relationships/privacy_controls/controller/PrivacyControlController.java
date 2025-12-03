package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.controller;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.BlockEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.MuteEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.payload.*;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.service.PrivacyControlService;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.utils.PrivacyControlMapper;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/e-social/privacy-control")
@RequiredArgsConstructor
public class PrivacyControlController {

    private final PrivacyControlService privacyControlService;
    private final PrivacyControlMapper privacyControlMapper;

    @PostMapping("/block/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> blockUser(@PathVariable UUID userId) {
        BlockEntity block = privacyControlService.blockUser(userId);
        BlockedUserResponse response = privacyControlMapper.toBlockedUserResponse(block);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User blocked successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/unblock/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> unblockUser(@PathVariable UUID userId) {
        privacyControlService.unblockUser(userId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User unblocked successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/mute/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> muteUser(@PathVariable UUID userId) {
        MuteEntity mute = privacyControlService.muteUser(userId);
        MutedUserResponse response = privacyControlMapper.toMutedUserResponse(mute);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User muted successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/unmute/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> unmuteUser(@PathVariable UUID userId) {
        privacyControlService.unmuteUser(userId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User unmuted successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/blocked")
    public ResponseEntity<GlobeSuccessResponseBuilder> getBlockedUsers() {
        List<BlockEntity> blockedUsers = privacyControlService.getBlockedUsers();
        List<BlockedUserResponse> response = privacyControlMapper.toBlockedUserResponseList(blockedUsers);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Blocked users retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/muted")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMutedUsers() {
        List<MuteEntity> mutedUsers = privacyControlService.getMutedUsers();
        List<MutedUserResponse> response = privacyControlMapper.toMutedUserResponseList(mutedUsers);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Muted users retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }
}