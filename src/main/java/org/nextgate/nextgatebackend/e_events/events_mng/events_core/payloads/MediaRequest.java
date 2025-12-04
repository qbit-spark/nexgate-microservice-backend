package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import jakarta.validation.constraints.Size;
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
public class MediaRequest {

    @Size(max = 500, message = "Banner URL must not exceed 500 characters")
    private String banner;

    @Size(max = 500, message = "Thumbnail URL must not exceed 500 characters")
    private String thumbnail;

    @Builder.Default
    private List<String> gallery = new ArrayList<>();
}