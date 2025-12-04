package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferGroupRequest {

    @NotNull(message = "Source group ID is required")
    private UUID sourceGroupId;

    @NotNull(message = "Target group ID is required")
    private UUID targetGroupId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}