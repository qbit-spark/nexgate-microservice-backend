package org.nextgate.nextgatebackend.authentication_service.payloads;

import lombok.Data;

@Data
public class VerifiedAccountResponse {

    private Object userData;
    private String accessToken;
    private String refreshToken;

}
