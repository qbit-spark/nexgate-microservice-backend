package org.nextgate.nextgatebackend.globe_api_client.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String errorMessage;
    private String exception;
    private int statusCode;
    private LocalDateTime timestamp = LocalDateTime.now();

    // Convenience methods
    public boolean isError() {
        return !success;
    }

    public boolean hasData() {
        return data != null;
    }
}
