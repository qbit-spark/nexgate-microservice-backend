package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PollRequest {

    @NotBlank(message = "Poll title is required")
    private String title;

    private String description;

    @NotNull(message = "Poll options are required")
    @Size(min = 2, max = 10, message = "Poll must have 2-10 options")
    private List<PollOptionRequest> options;

    private Boolean allowMultipleVotes = false;

    private Boolean isAnonymous = true;

    private Boolean allowVoteChange = true;

    private LocalDateTime expiresAt;
}