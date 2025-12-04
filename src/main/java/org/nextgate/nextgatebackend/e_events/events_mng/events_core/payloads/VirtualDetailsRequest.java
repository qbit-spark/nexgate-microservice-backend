package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualDetailsRequest {

    @Size(max = 500, message = "Meeting link must not exceed 500 characters")
    private String meetingLink;

    @Size(max = 100, message = "Meeting ID must not exceed 100 characters")
    private String meetingId;

    @Size(max = 100, message = "Passcode must not exceed 100 characters")
    private String passcode;
}
