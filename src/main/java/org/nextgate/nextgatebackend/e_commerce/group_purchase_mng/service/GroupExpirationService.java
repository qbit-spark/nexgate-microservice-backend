package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.service;

import java.util.UUID;

public interface GroupExpirationService {
    void expireGroup(UUID groupId);
}
