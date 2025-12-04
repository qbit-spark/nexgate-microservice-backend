package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.jobs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jobrunr.jobs.lambdas.JobRequest;

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

