package org.nextgate.nextgatebackend.files_mng_service.enums;

import lombok.Getter;

@Getter
public enum FileDirectory {
    PROFILE("profile"),
    CATEGORIES("categories"),
    SHOPS("shops_mng");

    private final String path;

    FileDirectory(String path) {
        this.path = path;
    }

}