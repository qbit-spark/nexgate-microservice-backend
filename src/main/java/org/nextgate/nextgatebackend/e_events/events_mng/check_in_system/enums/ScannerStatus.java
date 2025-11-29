package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums;

public enum ScannerStatus {
    ACTIVE, // - Currently active and scanning
    CLOSED, // - Session closed (scanner switched to different event)
    REVOKED, //- Blocked due to security violation
    EXPIRED  //- Credentials expired
}
