package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketCapacityRequest {

    @NotNull(message = "New total quantity is required")
    @Min(value = 0, message = "Total quantity must be zero or positive")
    private Integer newTotalQuantity;
}