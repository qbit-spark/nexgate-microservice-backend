package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketValidityType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    // ========== BASIC INFO ==========
    @NotBlank(message = "Ticket name is required")
    @Size(min = 2, max = 100, message = "Ticket name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be zero or positive")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimal places")
    private BigDecimal price;

    @Size(max = 3, message = "Currency code must be 3 characters")
    @Builder.Default
    private String currency = "USD";

    // ========== QUANTITY MANAGEMENT ==========
    @Min(value = 1, message = "Total quantity must be at least 1 if not unlimited")
    private Integer totalQuantity; // null if unlimited

    @Builder.Default
    private Boolean isUnlimited = false;

    // ========== SALES PERIOD (Optional) ==========
    private ZonedDateTime salesStartDateTime;

    private ZonedDateTime salesEndDateTime;

    // ========== PURCHASE LIMITS ==========
    @Min(value = 1, message = "Minimum quantity per order must be at least 1")
    @Builder.Default
    private Integer minQuantityPerOrder = 1;

    @Min(value = 1, message = "Maximum quantity per order must be at least 1")
    private Integer maxQuantityPerOrder; // null = no limit

    @Min(value = 1, message = "Maximum quantity per user must be at least 1")
    private Integer maxQuantityPerUser; // null = no limit

    // ========== TICKET VALIDITY ==========
    @NotNull(message = "Ticket validity type is required")
    @Builder.Default
    private TicketValidityType validUntilType = TicketValidityType.EVENT_END;

    private ZonedDateTime customValidUntil; // Required only if validUntilType = CUSTOM

    // ========== FOR HYBRID EVENTS ==========
    private AttendanceMode attendanceMode; // Required for HYBRID events

    // ========== INCLUSIVE ITEMS ==========
    @Builder.Default
    private List<String> inclusiveItems = new ArrayList<>();

    // ========== VISIBILITY ==========
    @Builder.Default
    private Boolean isHidden = false;
}