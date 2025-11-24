package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueRequest {

    @Size(max = 200, message = "Venue name must not exceed 200 characters")
    private String name;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;
}