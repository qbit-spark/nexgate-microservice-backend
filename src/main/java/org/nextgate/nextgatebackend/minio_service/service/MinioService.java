package org.nextgate.nextgatebackend.minio_service.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface MinioService {
    // Organization-based bucket operations
    void createOrganisationBucket(UUID organisationId);
    void deleteOrganisationBucket(UUID organisationId);
    boolean bucketExists(UUID organisationId);

    // File operations with organization context
    String uploadFile(UUID organisationId, String folderPath, String fileName, MultipartFile file);
    String uploadFile(UUID organisationId, String folderPath, String fileName,
                      InputStream inputStream, long size, String contentType);

    InputStream downloadFile(UUID organisationId, String objectKey);
    void deleteFile(UUID organisationId, String objectKey);
    boolean fileExists(UUID organisationId, String objectKey);

    // Folder operations
    void createFolderStructure(UUID organisationId, String folderPath);
    void deleteFolderStructure(UUID organisationId, String folderPath);
    List<String> listFolderContents(UUID organisationId, String folderPath);

    // Utility methods
    String generateObjectKey(String folderPath, String fileName);
    String getBucketName(UUID organisationId);
    long getFileSize(UUID organisationId, String objectKey);
    String getFileContentType(UUID organisationId, String objectKey);

    // Presigned URLs
    String generatePresignedDownloadUrl(UUID organisationId, String objectKey, int expirationInMinutes);
    String generatePresignedUploadUrl(UUID organisationId, String objectKey, int expirationInMinutes);

    // File operations
    void renameFile(UUID organisationId, String oldKey, String newKey);
    void copyFile(UUID organisationId, String sourceKey, String destinationKey);
}