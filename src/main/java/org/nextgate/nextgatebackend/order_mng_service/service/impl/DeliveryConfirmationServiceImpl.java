package org.nextgate.nextgatebackend.order_mng_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.order_mng_service.entity.DeliveryConfirmationEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.enums.ConfirmationStatus;
import org.nextgate.nextgatebackend.order_mng_service.repo.DeliveryConfirmationRepo;
import org.nextgate.nextgatebackend.order_mng_service.repo.OrderRepository;
import org.nextgate.nextgatebackend.order_mng_service.service.DeliveryConfirmationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryConfirmationServiceImpl implements DeliveryConfirmationService {

    private final DeliveryConfirmationRepo confirmationRepo;
    private final OrderRepository orderRepo;

    private static final int CODE_LENGTH = 6;
    private static final int CODE_VALIDITY_DAYS = 30;
    private static final int MAX_ATTEMPTS = 5;

    // ========================================
    // GENERATE CONFIRMATION CODE
    // ========================================

    @Override
    @Transactional
    public String generateConfirmationCode(OrderEntity order) {

        log.info("Generating delivery confirmation code for order: {}",
                order.getOrderNumber());

        // ========================================
        // 1. GENERATE RANDOM CODE
        // ========================================
        String plainCode = generateRandomCode(CODE_LENGTH);

        log.info("Plain code generated: {}", plainCode);

        // ========================================
        // 2. GENERATE SALT
        // ========================================
        String salt = generateSalt();

        // ========================================
        // 3. HASH THE CODE
        // ========================================
        String hashedCode = hashCode(plainCode, salt);

        log.info("Code hashed successfully");

        // ========================================
        // 4. CREATE CONFIRMATION ENTITY
        // ========================================
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(CODE_VALIDITY_DAYS);

        DeliveryConfirmationEntity confirmation = DeliveryConfirmationEntity.builder()
                .order(order)
                .codeHash(hashedCode)
                .salt(salt)
                .status(ConfirmationStatus.PENDING)
                .generatedAt(now)
                .expiresAt(expiresAt)
                .attemptCount(0)
                .maxAttempts(MAX_ATTEMPTS)
                .isRevoked(false)
                .build();

        confirmationRepo.save(confirmation);

        log.info("✓ Confirmation code generated");
        log.info("  Confirmation ID: {}", confirmation.getConfirmationId());
        log.info("  Expires At: {}", expiresAt);

        // ========================================
        // 5. RETURN PLAIN CODE (to send to customer)
        // ========================================
        return plainCode;
    }


    // ========================================
    // VERIFY CONFIRMATION CODE
    // ========================================

    @Override
    @Transactional
    public boolean verifyConfirmationCode(
            UUID orderId,
            String code,
            AccountEntity customer,
            String ipAddress,
            String deviceInfo
    ) throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║       VERIFYING DELIVERY CONFIRMATION CODE            ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Customer: {}", customer.getUserName());
        log.info("IP: {}", ipAddress);

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());

        // ========================================
        // 2. VALIDATE CUSTOMER IS BUYER
        // ========================================
        if (!order.getBuyer().getAccountId().equals(customer.getAccountId())) {
            log.warn("✗ Customer is not the buyer of this order");
            throw new BadRequestException("You are not the buyer of this order");
        }

        log.info("✓ Customer validated");

        // ========================================
        // 3. GET ACTIVE CONFIRMATION
        // ========================================
        DeliveryConfirmationEntity confirmation = confirmationRepo
                .findByOrderAndStatusAndIsRevokedFalse(
                        order,
                        ConfirmationStatus.PENDING
                )
                .orElseThrow(() -> new ItemNotFoundException(
                        "No active confirmation code found for this order"));

        log.info("Confirmation ID: {}", confirmation.getConfirmationId());
        log.info("Generated At: {}", confirmation.getGeneratedAt());
        log.info("Expires At: {}", confirmation.getExpiresAt());
        log.info("Attempts: {}/{}", confirmation.getAttemptCount(), confirmation.getMaxAttempts());

        // ========================================
        // 4. CHECK IF CAN ATTEMPT
        // ========================================
        if (!confirmation.canAttempt()) {
            log.warn("✗ Cannot attempt verification");

            if (confirmation.isExpired()) {
                confirmation.setStatus(ConfirmationStatus.EXPIRED);
                confirmationRepo.save(confirmation);
                throw new BadRequestException("Confirmation code has expired");
            }

            if (confirmation.getAttemptCount() >= confirmation.getMaxAttempts()) {
                confirmation.setStatus(ConfirmationStatus.MAX_ATTEMPTS);
                confirmationRepo.save(confirmation);
                throw new BadRequestException("Maximum verification attempts exceeded");
            }

            if (confirmation.getIsRevoked()) {
                throw new BadRequestException("Confirmation code has been revoked");
            }
        }

        // ========================================
        // 5. INCREMENT ATTEMPT COUNT
        // ========================================
        confirmation.incrementAttempt();
        confirmationRepo.save(confirmation);

        log.info("Attempt recorded: {}/{}",
                confirmation.getAttemptCount(),
                confirmation.getMaxAttempts());

        // ========================================
        // 6. HASH PROVIDED CODE
        // ========================================
        String providedCodeHash = hashCode(code.trim(), confirmation.getSalt());

        // ========================================
        // 7. COMPARE HASHES (timing-safe comparison)
        // ========================================
        boolean isValid = MessageDigest.isEqual(
                confirmation.getCodeHash().getBytes(StandardCharsets.UTF_8),
                providedCodeHash.getBytes(StandardCharsets.UTF_8)
        );

        if (!isValid) {
            log.warn("✗ Invalid confirmation code");

            int attemptsRemaining = confirmation.getMaxAttempts() -
                    confirmation.getAttemptCount();

            throw new BadRequestException(
                    String.format("Invalid confirmation code. %d attempts remaining.",
                            attemptsRemaining));
        }

        log.info("✓ Code verified successfully!");

        // ========================================
        // 8. MARK AS VERIFIED
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        confirmation.setStatus(ConfirmationStatus.VERIFIED);
        confirmation.setVerifiedAt(now);
        confirmation.setVerifiedBy(customer.getAccountId());
        confirmation.setVerificationIp(ipAddress);
        confirmation.setVerificationDevice(deviceInfo);

        confirmationRepo.save(confirmation);

        log.info("✓ Confirmation marked as VERIFIED");
        log.info("  Verified At: {}", now);
        log.info("  Verified By: {}", customer.getUserName());
        log.info("  IP: {}", ipAddress);

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║       VERIFICATION SUCCESSFUL                         ║");
        log.info("╚════════════════════════════════════════════════════════╝");

        return true;
    }


    // ========================================
    // GET ACTIVE CONFIRMATION
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public DeliveryConfirmationEntity getActiveConfirmation(UUID orderId)
            throws ItemNotFoundException {

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        return confirmationRepo
                .findByOrderAndStatusAndIsRevokedFalse(
                        order,
                        ConfirmationStatus.PENDING
                )
                .orElseThrow(() -> new ItemNotFoundException(
                        "No active confirmation found"));
    }


    // ========================================
    // REVOKE CONFIRMATION CODE
    // ========================================

    @Override
    @Transactional
    public void revokeConfirmationCode(UUID confirmationId, UUID revokedBy, String reason)
            throws ItemNotFoundException {

        log.info("Revoking confirmation code: {}", confirmationId);

        DeliveryConfirmationEntity confirmation = confirmationRepo.findById(confirmationId)
                .orElseThrow(() -> new ItemNotFoundException("Confirmation not found"));

        confirmation.setIsRevoked(true);
        confirmation.setRevokedAt(LocalDateTime.now());
        confirmation.setRevokedBy(revokedBy);
        confirmation.setRevocationReason(reason);
        confirmation.setStatus(ConfirmationStatus.REVOKED);

        confirmationRepo.save(confirmation);

        log.info("✓ Confirmation code revoked");
    }


    // ========================================
    // REGENERATE CONFIRMATION CODE
    // ========================================

    @Override
    @Transactional
    public String regenerateConfirmationCode(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException {

        log.info("Regenerating confirmation code for order: {}", orderId);

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        // Validate requester is buyer
        if (!order.getBuyer().getAccountId().equals(requester.getAccountId())) {
            throw new BadRequestException("You are not the buyer of this order");
        }

        // Revoke old confirmation (if exists)
        try {
            DeliveryConfirmationEntity oldConfirmation = getActiveConfirmation(orderId);
            revokeConfirmationCode(
                    oldConfirmation.getConfirmationId(),
                    requester.getAccountId(),
                    "Code regenerated by customer"
            );
        } catch (ItemNotFoundException e) {
            // No active confirmation, that's fine
        }

        // Generate new code
        return generateConfirmationCode(order);
    }


    // ========================================
    // HELPER METHODS
    // ========================================

    private String generateRandomCode(int length) {
        Random random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10)); // 0-9
        }

        return code.toString();
    }


    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }


    private String hashCode(String code, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = code + salt;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}