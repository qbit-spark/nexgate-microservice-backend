package org.nextgate.nextgatebackend.user_profile_service.service;

import jakarta.servlet.http.HttpServletRequest;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeadvice.exceptions.VerificationException;
import org.nextgate.nextgatebackend.user_profile_service.payload.*;

import java.util.UUID;

/**
 * Service interface for managing user profiles
 * Handles profile updates, security settings, and account management
 */
public interface UserProfileService {

    /**
     * Get current user profile information
     */
    UserProfileResponse getCurrentUserProfile(AccountEntity account) throws ItemNotFoundException;

    /**
     * Update basic user information (name, email, username)
     * Requires re-verification if email is changed
     */
    UserProfileResponse updateBasicInfo(AccountEntity account, UpdateBasicInfoRequest request)
            throws ItemNotFoundException, RandomExceptions;

    /**
     * Change user password with current password verification
     * Validates password strength and ensures new password is different
     */
    void changePassword(AccountEntity account, ChangePasswordRequest request, HttpServletRequest httpServletRequest)
            throws ItemNotFoundException, VerificationException, RandomExceptions;


    /**
     * Validate username availability and format
     * Returns suggestions if the username is taken
     */
    UsernameValidationResponse validateUsername(String username);

    /**
     * Request phone number verification via OTP
     * Updates phone number temporarily until verification
     */
    PhoneVerificationResponse requestPhoneVerification(UUID accountId, PhoneVerificationRequest request)
            throws ItemNotFoundException, RandomExceptions, VerificationException;

    /**
     * Verify phone number with OTP code
     * Marks phone as verified and saves permanently
     */
    UserProfileResponse verifyPhone(UUID accountId, VerifyPhoneRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions;

    /**
     * Request email verification via OTP
     * Only works if email is not already verified
     */
    EmailVerificationResponse requestEmailVerification(UUID accountId)
            throws ItemNotFoundException, RandomExceptions, VerificationException;

    /**
     * Verify email with OTP code
     * Marks email as verified
     */
    UserProfileResponse verifyEmail(UUID accountId, VerifyEmailRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions;

    /**
     * Get user security information and settings
     */
    UserSecurityInfoResponse getSecurityInfo(UUID accountId) throws ItemNotFoundException;

    /**
     * Enable two-factor authentication (simple boolean toggle)
     * Requires password verification
     */
    void enableTwoFactor(UUID accountId, EnableTwoFactorRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions;

    /**
     * Disable two-factor authentication (simple boolean toggle)
     * Requires password verification
     */
    void disableTwoFactor(UUID accountId, DisableTwoFactorRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions;

    /**
     * Deactivate user account (soft delete by locking)
     * Requires password confirmation
     */
    void deactivateAccount(UUID accountId, DeactivateAccountRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions;
}