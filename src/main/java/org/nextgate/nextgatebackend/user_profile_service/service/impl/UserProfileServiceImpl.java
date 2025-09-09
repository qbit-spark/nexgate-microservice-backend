package org.nextgate.nextgatebackend.user_profile_service.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.entity.Roles;
import org.nextgate.nextgatebackend.authentication_service.enums.TempTokenPurpose;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.authentication_service.service.TempTokenService;
import org.nextgate.nextgatebackend.emails_service.GlobeMailService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeadvice.exceptions.VerificationException;
import org.nextgate.nextgatebackend.user_profile_service.payload.*;
import org.nextgate.nextgatebackend.user_profile_service.service.UserProfileService;
import org.nextgate.nextgatebackend.user_profile_service.utils.ProfileValidationUtils;
import org.nextgate.nextgatebackend.user_profile_service.utils.UsernameHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final AccountRepo accountRepo;
    private final PasswordEncoder passwordEncoder;
    private final TempTokenService tempTokenService;
    private final GlobeMailService globeMailService;
    private final ProfileValidationUtils validationUtils;
    private final UsernameHelper usernameHelper;

    @Override
    public UserProfileResponse getCurrentUserProfile(AccountEntity account) throws ItemNotFoundException {
        return buildUserProfileResponse(account);
    }

    @Override
    @Transactional
    public UserProfileResponse updateBasicInfo(AccountEntity account, UpdateBasicInfoRequest request)
            throws ItemNotFoundException, RandomExceptions {

        boolean hasChanges = false;

        // Update username if provided and different
        if (request.getUserName() != null && !request.getUserName().equals(account.getUserName())) {
            validateAndUpdateUsername(account, request.getUserName());
            hasChanges = true;
        }

        // Update first name
        if (request.getFirstName() != null && !request.getFirstName().equals(account.getFirstName())) {
            account.setFirstName(validationUtils.sanitizeInput(request.getFirstName()));
            hasChanges = true;
        }

        // Update last name
        if (request.getLastName() != null && !request.getLastName().equals(account.getLastName())) {
            account.setLastName(validationUtils.sanitizeInput(request.getLastName()));
            hasChanges = true;
        }

        // Update middle name
        if (request.getMiddleName() != null && !request.getMiddleName().equals(account.getMiddleName())) {
            account.setMiddleName(validationUtils.sanitizeInput(request.getMiddleName()));
            hasChanges = true;
        }

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            validateAndUpdateEmail(account, request.getEmail());
            hasChanges = true;
        }

        // Update phone number if provided and different
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(account.getPhoneNumber())) {
            validateAndUpdatePhoneNumber(account, request.getPhoneNumber());
            hasChanges = true;
        }

        // Update profile picture URLs if provided and different
        if (request.getProfilePictureUrls() != null) {
            if (updateProfilePictureUrls(account, request.getProfilePictureUrls())) {
                hasChanges = true;
            }
        }

        if (hasChanges) {
            account.setEditedAt(LocalDateTime.now());
            AccountEntity savedAccount = accountRepo.save(account);
            return buildUserProfileResponse(savedAccount);
        }

        return buildUserProfileResponse(account);
    }

    @Override
    @Transactional
    public void changePassword(AccountEntity account, ChangePasswordRequest request, HttpServletRequest httpServletRequest)
            throws ItemNotFoundException, VerificationException, RandomExceptions {


        // Verify the current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new VerificationException("Current password is incorrect");
        }

        // Ensure the new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), account.getPassword())) {
            throw new RandomExceptions("New password must be different from current password");
        }

        // Validate new password strength
        ProfileValidationUtils.PasswordStrengthResult strengthResult =
                validationUtils.analyzePasswordStrength(request.getNewPassword());

        if (strengthResult.getScore() < 80) {
            throw new RandomExceptions("Password is not strong enough: " + strengthResult.getMessage());
        }

        // Update password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setEditedAt(LocalDateTime.now());
        accountRepo.save(account);


        // Send password change notification email
        sendPasswordChangeNotification(account, httpServletRequest);
    }


    @Override
    public UsernameValidationResponse validateUsername(String username) {
        UsernameHelper.UsernameValidationResult result =
                usernameHelper.validateUsernameDetailed(username);

        List<String> suggestions = new ArrayList<>();
        if (!result.isAvailable() && result.isValid()) {
            suggestions = usernameHelper.generateUsernameSuggestions(username, 5);
        }

        return UsernameValidationResponse.builder()
                .available(result.isAvailable())
                .valid(result.isValid())
                .message(result.getMessage())
                .suggestions(suggestions)
                .details(UsernameValidationResponse.ValidationDetails.builder()
                        .correctLength(result.getDetails().isCorrectLength())
                        .validFormat(result.getDetails().isValidFormat())
                        .notReserved(result.getDetails().isNotReserved())
                        .notTaken(result.getDetails().isNotTaken())
                        .formatRequirement(result.getDetails().getFormatRequirement())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public PhoneVerificationResponse requestPhoneVerification(UUID accountId, PhoneVerificationRequest request)
            throws ItemNotFoundException, RandomExceptions, VerificationException {

        AccountEntity account = getAccountById(accountId);

        // Validate a phone number format
        if (!validationUtils.isValidPhoneNumber(request.getPhoneNumber())) {
            throw new RandomExceptions("Invalid phone number format. Use international format (e.g., +1234567890)");
        }

        // Check if the phone number is already in use by another account
        validatePhoneNumberAvailability(request.getPhoneNumber(), accountId);

        // Update the phone number temporarily (will be confirmed after verification)
        account.setPhoneNumber(request.getPhoneNumber());
        account.setIsPhoneVerified(false);
        accountRepo.save(account);

        // Generate OTP and create temp token
        String otpCode = generateOtpCode();
        String tempToken = tempTokenService.createTempToken(
                account,
                TempTokenPurpose.PHONE_VERIFICATION_OTP,
                request.getPhoneNumber(),
                otpCode
        );

        // TODO: Implement SMS sending service
        log.info("Phone verification OTP: {} for user: {} (phone: {})",
                otpCode, accountId, validationUtils.maskPhoneNumber(request.getPhoneNumber()));

        return PhoneVerificationResponse.builder()
                .tempToken(tempToken)
                .message("OTP sent to your phone number")
                .expireAt(LocalDateTime.now().plusMinutes(10))
                .phoneNumber(validationUtils.maskPhoneNumber(request.getPhoneNumber()))
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse verifyPhone(UUID accountId, VerifyPhoneRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions {

        AccountEntity account = tempTokenService.validateTempTokenAndOTP(
                request.getTempToken(),
                request.getOtpCode()
        );

        if (!account.getId().equals(accountId)) {
            throw new VerificationException("Invalid verification token");
        }

        account.setIsPhoneVerified(true);
        account.setEditedAt(LocalDateTime.now());
        AccountEntity savedAccount = accountRepo.save(account);


        return buildUserProfileResponse(savedAccount);
    }

    @Override
    @Transactional
    public EmailVerificationResponse requestEmailVerification(UUID accountId)
            throws ItemNotFoundException, RandomExceptions, VerificationException {

        AccountEntity account = getAccountById(accountId);

        if (account.getIsEmailVerified()) {
            throw new VerificationException("Email is already verified");
        }

        String otpCode = generateOtpCode();
        String tempToken = tempTokenService.createTempToken(
                account,
                TempTokenPurpose.EMAIL_VERIFICATION_OTP,
                account.getEmail(),
                otpCode
        );

        // Send verification email
        try {
            globeMailService.sendOTPEmail(
                    account.getEmail(),
                    otpCode,
                    account.getFirstName(),
                    "Email Verification",
                    "Please use this OTP to verify your email address: "
            );
        } catch (Exception e) {
            throw new RandomExceptions("Failed to send verification email: " + e.getMessage());
        }

        EmailVerificationResponse response = EmailVerificationResponse.builder()
                .tempToken(tempToken)
                .message("Verification email sent successfully")
                .expireAt(LocalDateTime.now().plusMinutes(10))
                .email(validationUtils.maskEmail(account.getEmail()))
                .build();

        return response;
    }

    @Override
    @Transactional
    public UserProfileResponse verifyEmail(UUID accountId, VerifyEmailRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions {

        AccountEntity account = tempTokenService.validateTempTokenAndOTP(
                request.getTempToken(),
                request.getOtpCode()
        );

        if (!account.getId().equals(accountId)) {
            throw new VerificationException("Invalid verification token");
        }

        account.setIsEmailVerified(true);
        account.setEditedAt(LocalDateTime.now());
        AccountEntity savedAccount = accountRepo.save(account);

        log.info("Email {} verified successfully for user: {}",
                validationUtils.maskEmail(account.getEmail()), accountId);

        return buildUserProfileResponse(savedAccount);
    }

    @Override
    public UserSecurityInfoResponse getSecurityInfo(UUID accountId) throws ItemNotFoundException {
        AccountEntity account = getAccountById(accountId);

        // Calculate security strength
        UserSecurityInfoResponse.SecurityStrength securityStrength = calculateSecurityStrength(account);

        return UserSecurityInfoResponse.builder()
                .isEmailVerified(account.getIsEmailVerified())
                .isPhoneVerified(account.getIsPhoneVerified())
                .isTwoFactorEnabled(account.isTwoFactorEnabled())
                .isAccountLocked(account.isLocked())
                .lastPasswordChange(account.getEditedAt())
                .accountCreatedAt(account.getCreatedAt())
                .roles(account.getRoles().stream()
                        .map(Roles::getRoleName)
                        .collect(Collectors.toSet()))
                .securityStrength(securityStrength)
                .build();
    }

    @Override
    @Transactional
    public void enableTwoFactor(UUID accountId, EnableTwoFactorRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions {

        AccountEntity account = getAccountById(accountId);

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new VerificationException("Password is incorrect");
        }

        if (account.isTwoFactorEnabled()) {
            throw new RandomExceptions("Two-factor authentication is already enabled");
        }

        // Simply enable 2FA flag
        account.setTwoFactorEnabled(true);
        account.setEditedAt(LocalDateTime.now());
        accountRepo.save(account);

        log.info("Two-factor authentication enabled for user: {}", accountId);
    }

    @Override
    @Transactional
    public void disableTwoFactor(UUID accountId, DisableTwoFactorRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions {

        AccountEntity account = getAccountById(accountId);

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new VerificationException("Password is incorrect");
        }

        if (!account.isTwoFactorEnabled()) {
            throw new RandomExceptions("Two-factor authentication is not enabled");
        }

        // Simply disable 2FA flag
        account.setTwoFactorEnabled(false);
        account.setTwoFactorSecret(null); // Clear any existing secret
        account.setEditedAt(LocalDateTime.now());
        accountRepo.save(account);

        log.info("Two-factor authentication disabled for user: {}", accountId);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID accountId, DeactivateAccountRequest request)
            throws ItemNotFoundException, VerificationException, RandomExceptions {

        AccountEntity account = getAccountById(accountId);

        // Verify password before deactivation
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new VerificationException("Password is incorrect");
        }

        // Lock the account instead of deleting (for data integrity)
        account.setLocked(true);
        account.setLockedReason(request.getReason() != null ? request.getReason() : "Account deactivated by user");
        account.setEditedAt(LocalDateTime.now());
        accountRepo.save(account);

        log.info("Account deactivated for user: {}", accountId);

        // Send deactivation confirmation email
        sendAccountDeactivationNotification(account);
    }

    // ================= PRIVATE HELPER METHODS =================

    private AccountEntity getAccountById(UUID accountId) throws ItemNotFoundException {
        return accountRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("User account not found"));
    }

    private UserProfileResponse buildUserProfileResponse(AccountEntity account) {

        UserSecurityInfoResponse.SecurityStrength securityStrength = calculateSecurityStrength(account);


        return UserProfileResponse.builder()
                .id(account.getId())
                .userName(account.getUserName())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .middleName(account.getMiddleName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .isVerified(account.getIsVerified())
                .isEmailVerified(account.getIsEmailVerified())
                .isPhoneVerified(account.getIsPhoneVerified())
                .isTwoFactorEnabled(account.isTwoFactorEnabled())
                .isAccountLocked(account.isLocked())
                .createdAt(account.getCreatedAt())
                .editedAt(account.getEditedAt())
                .roles(account.getRoles().stream()
                        .map(Roles::getRoleName)
                        .collect(Collectors.toSet()))
                .profilePictureUrls(account.getProfilePictureUrls())
                .securityStrength(securityStrength)
                .build();
    }

    private boolean updateProfilePictureUrls(AccountEntity account, List<String> newUrls) {
        // Validate and sanitize URLs
        List<String> validatedUrls = validateProfilePictureUrls(newUrls);

        // Check if URLs are actually different
        List<String> currentUrls = account.getProfilePictureUrls();
        if (currentUrls == null) {
            currentUrls = new ArrayList<>();
        }

        // Compare lists (order matters)
        if (!validatedUrls.equals(currentUrls)) {
            account.setProfilePictureUrls(validatedUrls);
            log.info("Updated profile picture URLs for user: {} - {} URLs",
                    account.getUserName(), validatedUrls.size());
            return true;
        }

        return false;
    }

    /**
     * Validate profile picture URLs
     */
    private List<String> validateProfilePictureUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> validatedUrls = new ArrayList<>();

        for (String url : urls) {
            if (url != null && !url.trim().isEmpty()) {
                String sanitizedUrl = validationUtils.sanitizeInput(url.trim());

                // Validate URL format
                if (isValidImageUrl(sanitizedUrl)) {
                    validatedUrls.add(sanitizedUrl);
                } else {
                    log.warn("Invalid profile picture URL rejected: {}", sanitizedUrl);
                }
            }
        }

        // Limit number of profile pictures (e.g., max 5)
        if (validatedUrls.size() > 5) {
            log.warn("Too many profile picture URLs provided ({}), limiting to 5", validatedUrls.size());
            validatedUrls = validatedUrls.subList(0, 5);
        }

        return validatedUrls;
    }

    /**
     * Validate image URL format and security
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            // Basic URL format validation
            if (!url.matches("^https?://.*\\.(jpg|jpeg|png|gif|webp)(?:\\?.*)?$")) {
                return false;
            }

            // Security checks - prevent SSRF
//            String lowerUrl = url.toLowerCase();
//
//            // Block local/private URLs
//            if (lowerUrl.contains("localhost") ||
//                    lowerUrl.contains("127.0.0.1") ||
//                    lowerUrl.contains("192.168.") ||
//                    lowerUrl.contains("10.") ||
//                    lowerUrl.contains("172.")) {
//                log.warn("Blocked potentially unsafe URL: {}", url);
//                return false;
//            }

            // Check URL length
            if (url.length() > 500) {
                log.warn("URL too long: {} characters", url.length());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.warn("Error validating URL: {}", url, e);
            return false;
        }
    }

    /**
     * Validate and update phone number
     */
    private void validateAndUpdatePhoneNumber(AccountEntity account, String phoneNumber)
            throws RandomExceptions {

        String sanitizedPhone = validationUtils.sanitizeInput(phoneNumber);

        // Additional phone validation if needed
        if (!validationUtils.isValidPhoneNumber(sanitizedPhone)) {
            throw new RandomExceptions("Invalid phone number format");
        }

        // Check if the phone number is already taken by another user
        if (accountRepo.existsByPhoneNumberAndIdNot(sanitizedPhone, account.getId())) {
            throw new RandomExceptions("Phone number is already registered to another account");
        }

        account.setPhoneNumber(sanitizedPhone);
        // Reset phone verification status when phone changes
        account.setIsPhoneVerified(false);

        log.info("Phone number updated for user: {}", account.getUserName());
    }


    private void validateAndUpdateUsername(AccountEntity account, String newUsername) throws RandomExceptions {
        // Check if username is actually changing
        if (newUsername.equals(account.getUserName())) {
            return; // No change needed
        }

        // Validate username
        if (!validationUtils.isValidUsername(newUsername)) {
            throw new RandomExceptions("Invalid username format");
        }

        if (validationUtils.isReservedUsername(newUsername)) {
            throw new RandomExceptions("This username is reserved and cannot be used");
        }

        if (accountRepo.existsByUserName(newUsername)) {
            throw new RandomExceptions("Username is already taken");
        }

        account.setUserName(newUsername);
    }

    private void validateAndUpdateEmail(AccountEntity account, String newEmail) throws RandomExceptions {
        // Check if email is actually changing
        if (newEmail.equals(account.getEmail())) {
            return; // No change needed
        }

        // Validate email format
        if (!validationUtils.isValidEmail(newEmail)) {
            throw new RandomExceptions("Invalid email format");
        }

        // Check if email is already in use
        if (accountRepo.findByEmail(newEmail).isPresent()) {
            throw new RandomExceptions("Email is already in use");
        }

        account.setEmail(newEmail);
        account.setIsEmailVerified(false); // Require re-verification
    }

    private void validatePhoneNumberAvailability(String phoneNumber, UUID currentAccountId) throws RandomExceptions {
        accountRepo.findAccountEntitiesByEmailOrPhoneNumberOrUserName(null, phoneNumber, null)
                .ifPresent(existingAccount -> {
                    if (!existingAccount.getId().equals(currentAccountId)) {
                        throw new RuntimeException("Phone number is already in use by another account");
                    }
                });
    }

    private UserSecurityInfoResponse.SecurityStrength calculateSecurityStrength(AccountEntity account) {
        int score = 0;
        List<String> recommendations = new ArrayList<>();

        // Email verification (25 points)
        if (account.getIsEmailVerified()) {
            score += 25;
        } else {
            recommendations.add("Verify your email address");
        }

        // Phone verification (25 points)
        if (account.getIsPhoneVerified()) {
            score += 25;
        } else {
            recommendations.add("Verify your phone number");
        }

        // Two-factor authentication (35 points)
        if (account.isTwoFactorEnabled()) {
            score += 35;
        } else {
            recommendations.add("Enable two-factor authentication");
        }

        // Recent password change (15 points)
        if (account.getEditedAt() != null &&
                account.getEditedAt().isAfter(LocalDateTime.now().minusMonths(6))) {
            score += 15;
        } else {
            recommendations.add("Consider changing your password regularly");
        }

        String level;
        String description;

        if (score >= 80) {
            level = "STRONG";
            description = "Your account security is excellent";
        } else if (score >= 60) {
            level = "MEDIUM";
            description = "Your account security is good but can be improved";
        } else if (score >= 40) {
            level = "WEAK";
            description = "Your account security needs improvement";
        } else {
            level = "VERY_WEAK";
            description = "Your account security is poor and needs immediate attention";
        }

        return UserSecurityInfoResponse.SecurityStrength.builder()
                .score(score)
                .level(level)
                .description(description)
                .recommendations(recommendations)
                .build();
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int otp = random.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    private void sendPasswordChangeNotification(AccountEntity account, HttpServletRequest httpServletRequest) {
        try {
            globeMailService.sendPasswordChangeEmail(
                    account.getEmail(),
                    account.getFirstName(), "Password Change Alert",
                    "Your password has been successfully changed.",
                    httpServletRequest
            );
        } catch (Exception e) {
            log.warn("Failed to send password change notification email to user: {}", account.getId(), e);
        }
    }

    private void sendAccountDeactivationNotification(AccountEntity account) {
        try {
            globeMailService.sendOTPEmail(
                    account.getEmail(),
                    "",
                    account.getFirstName(),
                    "Account Deactivated",
                    "Your account has been deactivated successfully. If you want to reactivate it, please contact support."
            );
        } catch (Exception e) {
            log.warn("Failed to send deactivation confirmation email to user: {}", account.getId(), e);
        }
    }
}