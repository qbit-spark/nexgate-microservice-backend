package org.nextgate.nextgatebackend.group_purchase_mng.jobs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.nextgate.nextgatebackend.group_purchase_mng.service.GroupExpirationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ExpireGroupJobRequest implements JobRequest {

    private UUID groupId;

    @Override
    public Class<ExpireGroupJobRequestHandler> getJobRequestHandler() {
        return ExpireGroupJobRequestHandler.class;
    }
}

