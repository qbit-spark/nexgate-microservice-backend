package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualDetails {

    @Column(name = "virtual_meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "virtual_meeting_id", length = 100)
    private String meetingId;

    @Column(name = "virtual_passcode", length = 100)
    private String passcode;
}