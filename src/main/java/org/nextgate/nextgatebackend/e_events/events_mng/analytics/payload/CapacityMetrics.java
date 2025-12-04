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
    private Boolean hasUnlimitedTickets;
    private Integer limitedCapacity;
    private Integer limitedSold;
    private Integer limitedRemaining;
    private Integer unlimitedSold;
    private Integer totalSold;
    private Integer displayCapacity;
    private Double sellOutPercentage;
}