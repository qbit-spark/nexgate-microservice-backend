package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    private String banner;
    private String thumbnail;

    @Builder.Default
    private List<String> gallery = new ArrayList<>();
}