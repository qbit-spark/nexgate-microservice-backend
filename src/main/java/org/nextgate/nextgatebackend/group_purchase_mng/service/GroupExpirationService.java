package org.nextgate.nextgatebackend.group_purchase_mng.service;

import java.util.UUID;

public interface GroupExpirationService {
    void expireGroup(UUID groupId);
}
