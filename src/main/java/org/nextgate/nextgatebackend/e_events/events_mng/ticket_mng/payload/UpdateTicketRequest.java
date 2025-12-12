package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.CheckInValidityType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {

    @Size(min = 2, max = 100, message = "Ticket name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.00", message = "Price must be zero or positive")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimal places")
    private BigDecimal price;

    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQuantity;

    private ZonedDateTime salesStartDateTime;
    private ZonedDateTime salesEndDateTime;

    @Min(value = 1, message = "Minimum quantity per order must be at least 1")
    private Integer minQuantityPerOrder;

    @Min(value = 1, message = "Maximum quantity per order must be at least 1")
    private Integer maxQuantityPerOrder;

    @Min(value = 1, message = "Maximum quantity per user must be at least 1")
    private Integer maxQuantityPerUser;

    private CheckInValidityType checkInValidUntil;
    private ZonedDateTime customCheckInDate;

    private AttendanceMode attendanceMode;

    private List<String> inclusiveItems;
    private Boolean isHidden;
}