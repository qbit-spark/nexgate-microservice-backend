package org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityMetrics {
    private Integer totalCapacity;
    private Integer totalSold;
    private Integer totalRemaining;
    private Double sellOutPercentage;
}