package org.nextgate.nextgatebackend.emails_service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.emails_service.GlobeMailService;
import org.nextgate.nextgatebackend.user_profile_service.utils.SecurityInfoUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@RequiredArgsConstructor
@Service
@Slf4j
public class GlobeMailImpl implements GlobeMailService {

    private final EmailsHelperMethodsImpl emailsHelperMethodsIMPL;
    private final SecurityInfoUtils securityInfoUtils;

    @Override
    public void sendOTPEmail(String email, String otp, String userName, String textHeader, String instructions) throws Exception {
        try {
            log.info("Sending OTP email to: {} for user: {}", email, userName);

            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("emailHeader", textHeader);
            templateVariables.put("userName", userName != null ? userName : "User");
            templateVariables.put("instructionText", instructions);
            templateVariables.put("otpCode", otp);

            // Send email using template
            String subject = "Account Verification - Your OTP Code";
            emailsHelperMethodsIMPL.sendTemplateEmail(
                    email,
                    subject,
                    "verification_email",
                    templateVariables
            );

            log.info("OTP email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw new Exception("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordChangeEmail(String email, String userName, String header, String instructions,
                                        HttpServletRequest request) throws Exception {

        try {
            log.info("Sending password change notification email to: {} for user: {}", email, userName);

            // Extract security information from request using SecurityInfoUtils
            SecurityInfoUtils.SecurityInfo securityInfo = securityInfoUtils.extractSecurityInfo(request);

            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("emailHeader", header);
            templateVariables.put("userName", userName != null ? userName : "User");
            templateVariables.put("instructionText", instructions);

            // Security information
            templateVariables.put("requestTime", securityInfo.getFormattedRequestTime());
            templateVariables.put("ipAddress", securityInfo.getIpAddress());
            templateVariables.put("deviceInfo", securityInfo.getDeviceInfo());
            templateVariables.put("location", securityInfo.getLocation());

            // Send email using template
            String subject = "Password Changed Successfully - Nexgate";
            emailsHelperMethodsIMPL.sendTemplateEmail(
                    email,
                    subject,
                    "password_reset_email",
                    templateVariables
            );

            log.info("Password change notification email sent successfully to: {} from IP: {}",
                    email, securityInfo.getMaskedIpAddress()); // Use masked IP for logging

        } catch (Exception e) {
            log.error("Failed to send password change notification email to: {}", email, e);
            throw new Exception("Failed to send password change notification email: " + e.getMessage(), e);
        }
    }


}
