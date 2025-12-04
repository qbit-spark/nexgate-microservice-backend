package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venue {

    @Column(name = "venue_name", length = 200)
    private String name;

    @Column(name = "venue_address", length = 500)
    private String address;

    @Embedded
    private Coordinates coordinates;
}