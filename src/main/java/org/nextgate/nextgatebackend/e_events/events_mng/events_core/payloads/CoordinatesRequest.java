package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatesRequest {

    private BigDecimal latitude;

    private BigDecimal longitude;
}