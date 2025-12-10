package org.nextgate.nextgatebackend.files_mng_service.enums;

import lombok.Getter;

@Getter
public enum FileDirectory {
    PROFILE("profile"),
    CATEGORIES("categories"),
    SHOPS("shops"),
    PRODUCTS("products"),
    POSTS("posts"),
    EVENTS("events"),
    MESSAGES("messages"),
    DOCUMENTS("documents");

    private final String path;

    FileDirectory(String path) {
        this.path = path;
    }
}