package org.nextgate.nextgatebackend.user_profile_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.user_profile_service.payload.*;
import org.nextgate.nextgatebackend.user_profile_service.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AccountRepo accountRepo;

    @GetMapping("/me")
    public ResponseEntity<GlobeSuccessResponseBuilder> getCurrentUserProfile() throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();

        UserProfileResponse profileResponse = userProfileService.getCurrentUserProfile(account);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Profile retrieved successfully",
                profileResponse
        ));
    }

    //Todo: this action is dangerous, needs user to be logged in again/reauthenticate
    @PutMapping("/update-basic-info")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateBasicInfo(
            @Valid @RequestBody UpdateBasicInfoRequest request) throws Exception {

        AccountEntity account = getAuthenticatedAccount();

        // Check if the username is being changed
        boolean isUsernameChanging = request.getUserName() != null &&
                !request.getUserName().equals(account.getUserName());

        UserProfileResponse profileResponse = userProfileService.updateBasicInfo(account, request);

        // Simple message based on username change
        String message = isUsernameChanging
                ? "Username changed successfully! You will be logged out automatically. Please login again with your new username."
                : "Profile updated successfully";

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(message, profileResponse));
    }

    @PutMapping("/change-password")
    public ResponseEntity<GlobeSuccessResponseBuilder> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpServletRequest) throws Exception {


        AccountEntity account = getAuthenticatedAccount();

        userProfileService.changePassword(account, request, httpServletRequest);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Password changed successfully"
        ));
    }


    @GetMapping("/validate-username/{username}")
    public ResponseEntity<GlobeSuccessResponseBuilder> validateUsername(
            @PathVariable String username) {

        UsernameValidationResponse validation = userProfileService.validateUsername(username);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Username validation completed",
                validation
        ));
    }

    //For future use
    //@PostMapping("/request-phone-verification")
    public ResponseEntity<GlobeSuccessResponseBuilder> requestPhoneVerification(
            @Valid @RequestBody PhoneVerificationRequest request) throws Exception {

        UUID accountId = getAuthenticatedAccountId();

        PhoneVerificationResponse response = userProfileService.requestPhoneVerification(accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Phone verification OTP sent successfully",
                response
        ));
    }

    //For future use
    //@PostMapping("/verify-phone")
    public ResponseEntity<GlobeSuccessResponseBuilder> verifyPhone(
            @Valid @RequestBody VerifyPhoneRequest request) throws Exception {

        UUID accountId = getAuthenticatedAccountId();

        UserProfileResponse profileResponse = userProfileService.verifyPhone(accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Phone number verified successfully",
                profileResponse
        ));
    }

    @PostMapping("/request-email-verification")
    public ResponseEntity<GlobeSuccessResponseBuilder> requestEmailVerification() throws Exception {

        UUID accountId = getAuthenticatedAccountId();

        EmailVerificationResponse response = userProfileService.requestEmailVerification(accountId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Email verification OTP sent successfully",
                response
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<GlobeSuccessResponseBuilder> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) throws Exception {

        UUID accountId = getAuthenticatedAccountId();

        UserProfileResponse profileResponse = userProfileService.verifyEmail(accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Email verified successfully",
                profileResponse
        ));
    }

    @GetMapping("/security-info")
    public ResponseEntity<GlobeSuccessResponseBuilder> getSecurityInfo() throws ItemNotFoundException {
        UUID accountId = getAuthenticatedAccountId();

        UserSecurityInfoResponse securityInfo = userProfileService.getSecurityInfo(accountId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Security information retrieved successfully",
                securityInfo
        ));
    }

    @PostMapping("/enable-2fa")
    public ResponseEntity<GlobeSuccessResponseBuilder> enableTwoFactor(
            @Valid @RequestBody EnableTwoFactorRequest request) throws Exception {

        UUID accountId = getAuthenticatedAccountId();

        userProfileService.enableTwoFactor(accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Two-factor authentication enabled successfully"
        ));
    }

    @PostMapping("/disable-2fa")
    public ResponseEntity<GlobeSuccessResponseBuilder> disableTwoFactor(
            @Valid @RequestBody DisableTwoFactorRequest request) throws Exception {

        UUID accountId = getAuthenticatedAccountId();

        userProfileService.disableTwoFactor(accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Two-factor authentication disabled successfully"
        ));
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<GlobeSuccessResponseBuilder> deactivateAccount(
            @Valid @RequestBody DeactivateAccountRequest request) throws Exception {

        UUID accountId = getAuthenticatedAccountId();

        userProfileService.deactivateAccount(accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Account deactivated successfully"
        ));
    }

    private UUID getAuthenticatedAccountId() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            AccountEntity account = accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
            return account.getId();
        }
        throw new ItemNotFoundException("User not authenticated");
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