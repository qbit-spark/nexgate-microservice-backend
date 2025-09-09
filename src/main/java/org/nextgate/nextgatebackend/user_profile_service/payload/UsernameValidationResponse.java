package org.nextgate.nextgatebackend.user_profile_service.payload;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsernameValidationResponse {
    private boolean available;
    private boolean valid;
    private String message;
    private List<String> suggestions; // Multiple suggestions instead of single
    private ValidationDetails details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationDetails {
        private boolean correctLength;
        private boolean validFormat;
        private boolean notReserved;
        private boolean notTaken;
        private String formatRequirement;
    }
}
