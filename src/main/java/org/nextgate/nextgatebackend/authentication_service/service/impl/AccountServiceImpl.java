package org.nextgate.nextgatebackend.authentication_service.service.impl;


import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.entity.Roles;
import org.nextgate.nextgatebackend.authentication_service.enums.TempTokenPurpose;
import org.nextgate.nextgatebackend.authentication_service.enums.VerificationChannels;
import org.nextgate.nextgatebackend.authentication_service.payloads.AccountLoginRequest;
import org.nextgate.nextgatebackend.authentication_service.payloads.CreateAccountRequest;
import org.nextgate.nextgatebackend.authentication_service.payloads.RefreshTokenResponse;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.authentication_service.repo.RolesRepository;
import org.nextgate.nextgatebackend.authentication_service.service.AccountService;
import org.nextgate.nextgatebackend.authentication_service.service.TempTokenService;
import org.nextgate.nextgatebackend.authentication_service.utils.UsernameGenerationUtils;
import org.nextgate.nextgatebackend.emails_service.GlobeMailService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.TokenExpiredException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.TokenInvalidException;
import org.nextgate.nextgatebackend.globesecurity.JWTProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepo accountRepo;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTProvider tokenProvider;
    private final UsernameGenerationUtils usernameGenerationUtils;
    private final TempTokenService tempTokenService;
    private final GlobeMailService globeMailService;

    @Override
    public String registerAccount(CreateAccountRequest createAccountRequest) throws Exception {

        String generatedUsername = usernameGenerationUtils.generateUniqueUsernameFromEmail(createAccountRequest.getEmail());

        // Check the existence of a user by email, phone, or generated username
        if (accountRepo.existsByPhoneNumberOrEmailOrUserName(
                createAccountRequest.getPhoneNumber(),
                createAccountRequest.getEmail(),
                generatedUsername)) {
            throw new ItemReadyExistException("User with provided credentials already exist, please login");
        }

        AccountEntity account = new AccountEntity();
        account.setUserName(generatedUsername);
        account.setCreatedAt(LocalDateTime.now());
        account.setEditedAt(LocalDateTime.now());
        account.setIsVerified(false);
        account.setIsEmailVerified(false);
        account.setIsPhoneVerified(false);
        account.setFirstName(createAccountRequest.getFirstName());
        account.setLastName(createAccountRequest.getLastName());
        account.setMiddleName(createAccountRequest.getMiddleName());
        account.setEmail(createAccountRequest.getEmail());
        account.setPhoneNumber(createAccountRequest.getPhoneNumber());
        account.setPassword(passwordEncoder.encode(createAccountRequest.getPassword()));

        Set<Roles> roles = new HashSet<>();
        Roles userRoles = rolesRepository.findByRoleName("ROLE_NORMAL_USER")
                .orElseThrow(() -> new ItemNotFoundException("Default role not found"));
        roles.add(userRoles);
        account.setRoles(roles);

        AccountEntity savedAccount = accountRepo.save(account);

        String otpCode = generateOtpCode();

        String tempToken = tempTokenService.createTempToken(
                savedAccount,
                TempTokenPurpose.REGISTRATION_OTP,
                createAccountRequest.getEmail(),
                otpCode
        );


        return sendOTPViaChannel(createAccountRequest.getVerificationChannel(), savedAccount, otpCode, tempToken);
    }


    @Override
    public String loginAccount(AccountLoginRequest accountLoginRequest) throws Exception {

        String identifier = accountLoginRequest.getIdentifier();
        String password = accountLoginRequest.getPassword();

        AccountEntity userAccount = accountRepo.findByEmailOrPhoneNumberOrUserName(identifier, identifier, identifier).orElseThrow(() -> new ItemNotFoundException("User not found"));

        String otpCode = generateOtpCode();

        String tempToken = tempTokenService.createTempToken(
                userAccount,
                TempTokenPurpose.LOGIN_OTP,
                userAccount.getEmail(),
                otpCode
        );

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userAccount.getUserName(),
                        password));

        if (!userAccount.getIsVerified()) {
            userAccount.setIsVerified(true);
        }

        return sendOTPViaChannel(VerificationChannels.EMAIL, userAccount, otpCode, tempToken);

    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException {
        try {
            // First, validate that this is specifically a refresh token
            if (!tokenProvider.validToken(refreshToken, "REFRESH")) {
                throw new TokenInvalidException("Invalid token");
            }

            // Get username from a token
            String userName = tokenProvider.getUserName(refreshToken);

            // Retrieve user from database
            AccountEntity user = accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));

            // Create authentication with user authorities
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUserName(),
                    null,
                    mapRolesToAuthorities(user.getRoles())
            );

            // Generate only a new access token, not a new refresh token
            String newAccessToken = tokenProvider.generateAccessToken(authentication);

            // Build response
            RefreshTokenResponse refreshTokenResponse = new RefreshTokenResponse();
            refreshTokenResponse.setNewToken(newAccessToken);

            return refreshTokenResponse;

        } catch (TokenExpiredException e) {
            throw new TokenInvalidException("Refresh token has expired. Please login again");
        } catch (Exception e) {
            throw new TokenInvalidException("Failed to refresh token: " + e.getMessage());
        } finally {
            // Clear security context after token refresh
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public List<AccountEntity> getAllAccounts() {
        return accountRepo.findAll();
    }

    @Override
    public AccountEntity getAccountByID(UUID userId) throws ItemNotFoundException {
        return accountRepo.findById(userId).orElseThrow(() -> new ItemNotFoundException("No such user"));
    }

    private boolean isPhoneNumber(String input) {
        // Regular expression pattern for validating phone numbers
        String phoneRegex = "^\\+(?:[0-9] ?){6,14}[0-9]$";
        // Compile the pattern into a regex pattern object
        Pattern pattern = Pattern.compile(phoneRegex);
        // Use the pattern matcher to mcb_logo.png if the input matches the pattern
        return input != null && pattern.matcher(input).matches();
    }

    private boolean isEmail(String input) {
        // Regular expression pattern for validating email addresses
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        // Compile the pattern into a regex pattern object
        Pattern pattern = Pattern.compile(emailRegex);
        // Use the pattern matcher to mcb_logo.png if the input matches the pattern
        return input != null && pattern.matcher(input).matches();
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Roles> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }

    private String generateOtpCode() {
        Random random = new Random();
        int otp = random.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    private String sendOTPViaChannel(VerificationChannels verificationChannels, AccountEntity savedAccount, String otpCode, String tempToken) throws Exception {
        //Check a selected verification channel
        switch (verificationChannels) {
            case EMAIL -> //Send the OTP via email
                    globeMailService.sendOTPEmail(savedAccount.getEmail(), otpCode, savedAccount.getFirstName(), "Welcome to BuildWise Books Support!", "Please use the following OTP to complete your Authentication: ");
            case SMS -> {
                System.out.println("SMS verification is not implemented yet.");
            }
            case EMAIL_AND_SMS -> {
                System.out.println("Email and SMS verification is not implemented yet.");
            }

            case SMS_AND_WHATSAPP -> {
                System.out.println("SMS and WhatsApp verification is not implemented yet.");
            }
            case WHATSAPP -> {
                System.out.println("WhatsApp verification is not implemented yet.");
            }
            case VOICE_CALL -> {
                System.out.println("Voice call verification is not implemented yet.");
            }
            case PUSH_NOTIFICATION -> {
                System.out.println("Push notification verification is not implemented yet.");
            }
            case ALL_CHANNELS -> {
                System.out.println("All channels verification is not implemented yet.");
            }

            default ->
                    globeMailService.sendOTPEmail(savedAccount.getEmail(), otpCode, savedAccount.getFirstName(), "Welcome to BuildWise Books Support!", "Please use the following OTP to complete your registration: ");

        }

        return tempToken;
    }

}
