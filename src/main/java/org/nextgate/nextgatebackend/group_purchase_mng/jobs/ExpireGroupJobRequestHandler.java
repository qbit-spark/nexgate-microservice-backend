package org.nextgate.nextgatebackend.group_purchase_mng.jobs;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.nextgate.nextgatebackend.group_purchase_mng.service.GroupExpirationService;
import org.springframework.stereotype.Component;

@Component
public class ExpireGroupJobRequestHandler implements JobRequestHandler<ExpireGroupJobRequest> {

    private final GroupExpirationService groupExpirationService;

    public ExpireGroupJobRequestHandler(GroupExpirationService groupExpirationService) {
        this.groupExpirationService = groupExpirationService;
    }

    @Override
    public void run(ExpireGroupJobRequest jobRequest) {
        groupExpirationService.expireGroup(jobRequest.getGroupId());
    }
}
