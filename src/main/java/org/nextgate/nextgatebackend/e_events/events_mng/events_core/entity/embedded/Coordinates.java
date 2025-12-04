package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded;

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
public class Coordinates {

    @Column(name = "venue_latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "venue_longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
}