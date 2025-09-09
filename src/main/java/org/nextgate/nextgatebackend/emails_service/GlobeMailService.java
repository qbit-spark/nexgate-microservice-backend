package org.nextgate.nextgatebackend.emails_service;

import jakarta.servlet.http.HttpServletRequest;

public interface GlobeMailService {
    void sendOTPEmail(String email, String otp, String userName, String header, String instructions) throws Exception;

    void sendPasswordChangeEmail(String email, String userName, String header, String instructions,
                                 HttpServletRequest request) throws Exception;

}
