package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums;

public enum ScannerStatus {
    ACTIVE,   // Currently active and can scan
    REVOKED   // Permanently blocked (security issue, stolen credentials, etc.)
}