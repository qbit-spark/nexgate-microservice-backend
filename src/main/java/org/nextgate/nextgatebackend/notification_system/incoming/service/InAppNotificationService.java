package org.nextgate.nextgatebackend.notification_system.incoming.service;


import org.nextgate.nextgatebackend.notification_system.incoming.payloads.InAppNotificationRequest;

import java.util.UUID;

public interface InAppNotificationService {

    UUID saveNotification(InAppNotificationRequest request);
}