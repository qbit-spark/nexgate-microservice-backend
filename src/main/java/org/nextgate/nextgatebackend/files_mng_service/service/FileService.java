package org.nextgate.nextgatebackend.files_mng_service.service;


import org.nextgate.nextgatebackend.files_mng_service.enums.FileDirectory;
import org.nextgate.nextgatebackend.files_mng_service.payload.FileResponse;
import org.nextgate.nextgatebackend.files_mng_service.payload.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileService {

    FileUploadResponse uploadFiles(UUID accountId, FileDirectory directory, MultipartFile[] files);

    FileResponse uploadSingleFile(UUID accountId, FileDirectory directory, MultipartFile file);

    List<FileResponse> getUserFiles(UUID accountId, FileDirectory directory);

    void deleteFile(UUID accountId, String objectKey);


}