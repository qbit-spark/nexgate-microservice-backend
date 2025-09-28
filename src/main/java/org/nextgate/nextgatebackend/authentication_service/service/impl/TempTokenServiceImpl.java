package org.nextgate.nextgatebackend.authentication_service.service.impl;

import lombok.extern.slf4j.Slf4j;
import  org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import  org.nextgate.nextgatebackend.authentication_service.entity.TempTokenEntity;
import  org.nextgate.nextgatebackend.authentication_service.enums.TempTokenPurpose;
import  org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import  org.nextgate.nextgatebackend.authentication_service.repo.TempTokenRepository;
import org.nextgate.nextgatebackend.authentication_service.service.TempTokenService;
import  org.nextgate.nextgatebackend.emails_service.GlobeMailService;
import  org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import  org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import  org.nextgate.nextgatebackend.globeadvice.exceptions.VerificationException;
import  org.nextgate.nextgatebackend.globesecurity.JWTProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.minio_service.service.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TempTokenServiceImpl implements TempTokenService {

    private final TempTokenRepository tempTokenRepository;
    private final JWTProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepo accountRepo;
    private final GlobeMailService globeMailService;

    @Value("${temp.token.expiry.minutes:10}")
    private int tempTokenExpiryMinutes;

    @Value("${temp.token.rate.limit.count:5}")
    private int rateLimitCount;

    @Value("${temp.token.rate.limit.window.minutes:10}")
    private int rateLimitWindowMinutes;

    @Value("${temp.token.resend.limit.count:5}")
    private int resendLimitCount;

    @Value("${temp.token.resend.cooldown.minutes:2}")
    private int resendCooldownMinutes;

    private final MinioService minioService;


    @Override
    @Transactional
    public String createTempToken(AccountEntity account, TempTokenPurpose purpose, String identifier, String otpCode) throws RandomExceptions {

        // For registration, account will be null
        // For login/password reset, account will be provided
        String userIdentifier = (account != null) ? account.getEmail() : identifier;

        // Check rate limiting
        if (!isWithinRateLimit(account, userIdentifier, purpose)) {
            throw new RandomExceptions("Too many OTP requests. Please wait before requesting again.");
        }

        // Invalidate any existing active tokens for the same purpose
        invalidateAllTokensForPurpose(account, userIdentifier, purpose);

        // Create a JWT payloads
        Map<String, Object> claims = new HashMap<>();
        claims.put("userIdentifier", userIdentifier);
        claims.put("purpose", purpose.name());
        claims.put("identifier", identifier);
        if (account != null) {
            claims.put("userId", account.getId().toString());
        }
        claims.put("exp", System.currentTimeMillis() + ((long) tempTokenExpiryMinutes * 60 * 1000));

        // Generate JWT token
        String jwtToken = jwtProvider.generateTempToken(claims);

        // Hash the token for database storage
        String tokenHash = hashString(jwtToken);

        // Hash the OTP for secure storage
        String otpHash = passwordEncoder.encode(otpCode);

        // Create temp token entity
        TempTokenEntity tempToken = new TempTokenEntity();
        tempToken.setTokenHash(tokenHash);
        tempToken.setPurpose(purpose);
        tempToken.setIdentifier(identifier);
        tempToken.setUserIdentifier(userIdentifier);  // ✅ Always store for auditing
        tempToken.setOtpHash(otpHash);
        tempToken.setAccount(account);  // ✅ Can be null for registration
        tempToken.setCreatedAt(LocalDateTime.now());
        tempToken.setExpiresAt(LocalDateTime.now().plusMinutes(tempTokenExpiryMinutes));

        tempTokenRepository.save(tempToken);

        return jwtToken;
    }

    @Override
    @Transactional
    public AccountEntity validateTempTokenAndOTP(String tempToken, String otpCode) throws VerificationException, RandomExceptions {

        // Hash the provided token to find it in a database
        String tokenHash = hashString(tempToken);

        // Find the temp token
        TempTokenEntity tempTokenEntity = tempTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new VerificationException("Invalid or expired temporary token"));

        // Check if a token is already used
        if (tempTokenEntity.getIsUsed()) {
            throw new VerificationException("Token has already been used");
        }

        // Check if the token is expired
        if (tempTokenEntity.isExpired()) {
            throw new VerificationException("Token has expired");
        }

        // Check if max attempts reached
        if (tempTokenEntity.isMaxAttemptsReached()) {
            throw new VerificationException("Maximum verification attempts exceeded");
        }

        // Verify OTP
        if (!passwordEncoder.matches(otpCode, tempTokenEntity.getOtpHash())) {
            // Increment failed attempts
            tempTokenEntity.incrementAttempts();
            tempTokenRepository.save(tempTokenEntity);
            throw new VerificationException("Invalid OTP code");
        }

        // Mark token as used
        tempTokenEntity.markAsUsed();
        tempTokenRepository.save(tempTokenEntity);

        AccountEntity account = tempTokenEntity.getAccount();

        //Todo: Take action based in purpose of token
        switch (tempTokenEntity.getPurpose()) {

            case REGISTRATION_OTP -> actAfterRegistrationOtpValid(account);
            case LOGIN_OTP -> actAfterLoginOtpValid(account);


        }

        return account;
    }

    @Override
    @Transactional
    public String resendOTP(String tempToken) throws VerificationException, RandomExceptions {

        // Validate and decode the temp token first
        Claims claims;
        try {
            if (!jwtProvider.validateTempToken(tempToken, "")) {
                throw new VerificationException("Invalid temporary token");
            }
            claims = jwtProvider.getTempTokenClaims(tempToken);
        } catch (Exception e) {
            throw new VerificationException("Invalid or expired temporary token");
        }

        String userIdentifier = claims.get("userIdentifier", String.class);
        String purpose = claims.get("purpose", String.class);
        String identifier = claims.get("identifier", String.class);

        TempTokenPurpose tokenPurpose = TempTokenPurpose.valueOf(purpose);

        // Security checks
        if (!canResendOTP(userIdentifier, tokenPurpose)) {
            throw new RandomExceptions("Resend limit exceeded. Please wait before requesting again.");
        }

        // Find the original temp token in a database
        String tokenHash = hashString(tempToken);
        TempTokenEntity originalToken = tempTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new VerificationException("Token not found or already used"));

        // Additional security checks
        if (originalToken.getIsUsed()) {
            throw new VerificationException("Token has already been used");
        }

        if (originalToken.isExpired()) {
            throw new VerificationException("Token has expired");
        }

        // Check a cooldown period
        LocalDateTime lastResend = getLastResendTime(userIdentifier, tokenPurpose);
        if (lastResend != null &&
                LocalDateTime.now().isBefore(lastResend.plusMinutes(resendCooldownMinutes))) {
            throw new RandomExceptions("Please wait " + resendCooldownMinutes +
                    " minutes before requesting another OTP");
        }

        // Get an account (can be null for registration)
        AccountEntity account = originalToken.getAccount();

        // Generate new OTP
        String newOtpCode = generateOtpCode();

        // Invalidate the old token
        originalToken.markAsUsed();
        tempTokenRepository.save(originalToken);

        // Create a new temp token with new OTP
        String newTempToken = createTempToken(account, tokenPurpose, identifier, newOtpCode);

        // Send OTP based on purpose and channel
        sendOTPBasedOnPurpose(tokenPurpose, identifier, newOtpCode, account);

        return newTempToken;
    }


    @Override
    @Transactional
    public void invalidateAllTokensForPurpose(AccountEntity account, TempTokenPurpose purpose) {
        invalidateAllTokensForPurpose(account, null, purpose);
    }


    @Override
    @Transactional
    public String sendPSWDResetOTP(String email) throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Find an account by email
        AccountEntity account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new ItemNotFoundException("No account found with this email"));

        if (!account.getIsVerified()) {
            throw new VerificationException("Account is not verified. Please complete registration first.");
        }

        if (!account.getIsEmailVerified()) {
            throw new VerificationException("Email is not verified. Please verify your email first.");
        }

        // Check rate limiting
        if (!isWithinRateLimit(account, TempTokenPurpose.PASSWORD_RESET_OTP)) {
            throw new RandomExceptions("Too many password reset requests. Please wait before requesting again.");
        }

        // Invalidate any existing password reset tokens
        invalidateAllTokensForPurpose(account, TempTokenPurpose.PASSWORD_RESET_OTP);

        // Generate new OTP
        String newOtpCode = generateOtpCode();

        // Create temp token
        String tempToken = createTempToken(
                account,
                TempTokenPurpose.PASSWORD_RESET_OTP,
                email,
                newOtpCode
        );

        // Send password reset OTP
        try {
            globeMailService.sendOTPEmail(
                    email,
                    newOtpCode,
                    account.getFirstName(),
                    "Password Reset Request",
                    "Please use this OTP to reset your password: "
            );
        } catch (Exception e) {
            throw new RandomExceptions("Failed to send password reset email: " + e.getMessage());
        }

        return tempToken;
    }


    @Transactional
    public void invalidateAllTokensForPurpose(AccountEntity account, String userIdentifier, TempTokenPurpose purpose) {
        List<TempTokenEntity> activeTokens;

        if (account != null) {
            // For login/password reset - find by account
            activeTokens = tempTokenRepository.findByAccountAndPurposeAndIsUsed(account, purpose, false);
        } else {
            // For registration - find by userIdentifier
            activeTokens = tempTokenRepository.findByUserIdentifierAndPurposeAndIsUsed(userIdentifier, purpose, false);
        }

        // Mark them all as used
        for (TempTokenEntity token : activeTokens) {
            token.markAsUsed();
        }

        if (!activeTokens.isEmpty()) {
            tempTokenRepository.saveAll(activeTokens);
        }
    }

    @Override
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();

        // Delete expired tokens
        List<TempTokenEntity> expiredTokens = tempTokenRepository.findByExpiresAtBefore(now);
        if (!expiredTokens.isEmpty()) {
            tempTokenRepository.deleteAll(expiredTokens);
        }

        // Delete old used tokens (older than 1 day)
        LocalDateTime cutoffTime = now.minusDays(1);
        List<TempTokenEntity> oldUsedTokens = tempTokenRepository
                .findByIsUsedAndCreatedAtBefore(true, cutoffTime);
        if (!oldUsedTokens.isEmpty()) {
            tempTokenRepository.deleteAll(oldUsedTokens);
        }
    }

    @Override
    public boolean isWithinRateLimit(AccountEntity account, TempTokenPurpose purpose) {
        String userIdentifier = (account != null) ? account.getEmail() : null;
        return isWithinRateLimit(account, userIdentifier, purpose);
    }

    // Overloaded method to handle both scenarios
    public boolean isWithinRateLimit(AccountEntity account, String userIdentifier, TempTokenPurpose purpose) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);

        List<TempTokenEntity> recentTokens;

        if (account != null) {
            // For login/password reset - check by account
            recentTokens = tempTokenRepository.findByAccountAndPurposeAndCreatedAtAfter(account, purpose, windowStart);
        } else {
            // For registration - check by userIdentifier
            recentTokens = tempTokenRepository.findByUserIdentifierAndPurposeAndCreatedAtAfter(userIdentifier, purpose, windowStart);
        }

        return recentTokens.size() < rateLimitCount;
    }

    /**
     * Hash string using SHA-256
     */
    private String hashString(String input) throws RandomExceptions {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RandomExceptions("Error hashing token: " + e.getMessage());
        }
    }

    private void actAfterRegistrationOtpValid(AccountEntity account) {
        // Mark the user as verified and set email as verified
        account.setIsVerified(true);
        account.setIsEmailVerified(true);
        account.setEditedAt(LocalDateTime.now());

        if(!account.isBucketCreated()){
            createUserBucket(account);
        }

        // Save the updated account
        accountRepo.save(account);
    }

    private void actAfterLoginOtpValid(AccountEntity account) {

        account.setIsVerified(true);
        account.setIsEmailVerified(true);

        // Check if bucket needs to be created (for existing users who didn't have buckets)
        if(!account.isBucketCreated()){
            createUserBucket(account);
            accountRepo.save(account);
        }
    }


    @Override
    public boolean canResendOTP(String userIdentifier, TempTokenPurpose purpose) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);

        List<TempTokenEntity> recentTokens = tempTokenRepository
                .findByUserIdentifierAndPurposeAndCreatedAtAfter(userIdentifier, purpose, windowStart);

        return recentTokens.size() < resendLimitCount;
    }


    @Override
    public int getRemainingResendAttempts(String userIdentifier, TempTokenPurpose purpose) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);

        List<TempTokenEntity> recentTokens = tempTokenRepository
                .findByUserIdentifierAndPurposeAndCreatedAtAfter(userIdentifier, purpose, windowStart);

        return Math.max(0, resendLimitCount - recentTokens.size());
    }

    @Override

    public LocalDateTime getNextResendAllowedTime(String userIdentifier, TempTokenPurpose purpose) {
        LocalDateTime lastResend = getLastResendTime(userIdentifier, purpose);

        if (lastResend == null) {
            return LocalDateTime.now(); // Can resend immediately
        }

        return lastResend.plusMinutes(resendCooldownMinutes);
    }



    private LocalDateTime getLastResendTime(String userIdentifier, TempTokenPurpose purpose) {
        List<TempTokenEntity> recentTokens = tempTokenRepository
                .findByUserIdentifierAndPurposeAndCreatedAtAfter(
                        userIdentifier,
                        purpose,
                        LocalDateTime.now().minusHours(1)
                );

        return recentTokens.stream()
                .map(TempTokenEntity::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private void sendOTPBasedOnPurpose(TempTokenPurpose purpose, String identifier,
                                       String otpCode, AccountEntity account) throws RandomExceptions {

        try {
            switch (purpose) {
                case REGISTRATION_OTP -> {
                    String firstName = (account != null) ? account.getFirstName() : "User";
                    globeMailService.sendOTPEmail(
                            identifier,
                            otpCode,
                            firstName,
                            "Welcome to BuildWise Books Support!",
                            "Please use the following OTP to complete your registration: "
                    );
                }
                case PASSWORD_RESET_OTP -> {
                    String firstName = (account != null) ? account.getFirstName() : "User";
                    globeMailService.sendOTPEmail(
                            identifier,
                            otpCode,
                            firstName,
                            "Password Reset Request",
                            "Please use the following OTP to reset your password: "
                    );
                }
                case LOGIN_OTP -> {
                    String firstName = (account != null) ? account.getFirstName() : "User";
                    globeMailService.sendOTPEmail(
                            identifier,
                            otpCode,
                            firstName,
                            "Login Verification",
                            "Please use the following OTP to complete your login: "
                    );
                }
                default -> throw new RandomExceptions("Unsupported token purpose for resend");
            }
        } catch (Exception e) {
            throw new RandomExceptions("Failed to send OTP: " + e.getMessage());
        }
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int otp = random.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    @Transactional
    protected String handleRegistrationResendByEmail(String email)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Find an account by email
        AccountEntity account = accountRepo.findByEmail(email).orElse(null);

        if (account == null) {
            throw new ItemNotFoundException(
                    "No registration found for this email. Please start registration process."
            );
        }

        if (account.getIsVerified()) {
            throw new VerificationException(
                    "Account is already verified. You can login directly."
            );
        }

        // Invalidate any existing active registration tokens for this email
        invalidateAllTokensForPurpose(account, TempTokenPurpose.REGISTRATION_OTP);

        // Generate fresh OTP and token
        String newOtpCode = generateOtpCode();
        String newTempToken = createTempToken(
                account,
                TempTokenPurpose.REGISTRATION_OTP,
                email,
                newOtpCode
        );

        // Send OTP via email
        try {
            globeMailService.sendOTPEmail(
                    email,
                    newOtpCode,
                    account.getFirstName(),
                    "Complete Your Registration",
                    "Welcome back! Please use this OTP to complete your registration: "
            );
        } catch (Exception e) {
            throw new RandomExceptions("Failed to send verification email: " + e.getMessage());
        }

        return newTempToken;
    }

    @Transactional
    protected String handlePasswordResetResendByEmail(String email)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Find account by email
        AccountEntity account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new ItemNotFoundException("No account found with this email"));

        if (!account.getIsVerified()) {
            throw new VerificationException(
                    "Account is not verified. Please complete registration first."
            );
        }

        // Invalidate existing password reset tokens
        invalidateAllTokensForPurpose(account, TempTokenPurpose.PASSWORD_RESET_OTP);

        // Generate fresh OTP and token
        String newOtpCode = generateOtpCode();
        String newTempToken = createTempToken(
                account,
                TempTokenPurpose.PASSWORD_RESET_OTP,
                email,
                newOtpCode
        );

        // Send password reset OTP
        try {
            globeMailService.sendOTPEmail(
                    email,
                    newOtpCode,
                    account.getFirstName(),
                    "Password Reset Request",
                    "Please use this OTP to reset your password: "
            );
        } catch (Exception e) {
            throw new RandomExceptions("Failed to send password reset email: " + e.getMessage());
        }

        return newTempToken;
    }

    @Transactional
    protected String handleLoginResendByEmail(String email)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        AccountEntity account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new ItemNotFoundException("No account found with this email"));

        if (!account.getIsVerified()) {
            throw new VerificationException(
                    "Account is not verified. Please complete registration first."
            );
        }

        // Generate fresh login OTP
        String newOtpCode = generateOtpCode();
        String newTempToken = createTempToken(
                account,
                TempTokenPurpose.LOGIN_OTP,
                email,
                newOtpCode
        );

        // Send login OTP
        try {
            globeMailService.sendOTPEmail(
                    email,
                    newOtpCode,
                    account.getFirstName(),
                    "Login Verification",
                    "Please use this OTP to complete your login: "
            );
        } catch (Exception e) {
            throw new RandomExceptions("Failed to send login verification email: " + e.getMessage());
        }

        return newTempToken;
    }

    @Override
    public boolean canResendByEmail(String email, TempTokenPurpose purpose) {
        // Use the same rate limiting logic as token-based resend
        return canResendOTP(email, purpose);
    }

    private void createUserBucket(AccountEntity account) {
        try {
            minioService.createOrganisationBucket(account.getId());

            // Create basic folder structure
            minioService.createFolderStructure(account.getId(), "profile");


            // Mark bucket as created
            account.setBucketCreated(true);

            log.info("MinIO bucket created successfully for user: {}");

        } catch (Exception e) {
            log.error("Failed to create MinIO bucket for user: {}, Error: {}",
                    account.getId(), e.getMessage());
            // Keep bucketCreated as false for retry later
        }
    }

}