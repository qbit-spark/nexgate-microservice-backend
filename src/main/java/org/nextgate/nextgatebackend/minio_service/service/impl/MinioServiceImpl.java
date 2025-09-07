package org.nextgate.nextgatebackend.minio_service.service.impl;


import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.minio_service.config.MinioConfig;
import org.nextgate.nextgatebackend.minio_service.service.MinioService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public void createOrganisationBucket(UUID organisationId) {
        try {
            String bucketName = getBucketName(organisationId);
            if (!bucketExists(organisationId)) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Created organisation bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error creating bucket for organisation: {}", organisationId, e);
            throw new RuntimeException("Failed to create organisation bucket", e);
        }
    }

    @Override
    public boolean bucketExists(UUID organisationId) {
        try {
            String bucketName = getBucketName(organisationId);
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error checking bucket existence for organisation: {}", organisationId, e);
            return false;
        }
    }

    @Override
    public void deleteOrganisationBucket(UUID organisationId) {
        try {
            String bucketName = getBucketName(organisationId);
            // First, remove all objects in bucket
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );

            List<DeleteObject> objectsToDelete = new ArrayList<>();
            for (Result<Item> result : results) {
                objectsToDelete.add(new DeleteObject(result.get().objectName()));
            }

            if (!objectsToDelete.isEmpty()) {
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(bucketName)
                                .objects(objectsToDelete)
                                .build()
                );
            }

            // Remove bucket
            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("Deleted organisation bucket: {}", bucketName);
        } catch (Exception e) {
            log.error("Error deleting bucket for organisation: {}", organisationId, e);
            throw new RuntimeException("Failed to delete organisation bucket", e);
        }
    }

    @Override
    public String uploadFile(UUID organisationId, String folderPath, String fileName, MultipartFile file) {
        try {
            return uploadFile(organisationId, folderPath, fileName, file.getInputStream(),
                    file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("Error reading multipart file", e);
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }

    @Override
    public String uploadFile(UUID organisationId, String folderPath, String fileName,
                             InputStream inputStream, long size, String contentType) {
        try {
            // Ensure bucket exists
            createOrganisationBucket(organisationId);

            String objectKey = generateObjectKey(folderPath, fileName);

            String bucketName = getBucketName(organisationId);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );

            log.info("Uploaded file: {} to organisation bucket: {}", objectKey, bucketName);
            return objectKey;

        } catch (Exception e) {
            log.error("Error uploading file for organisation: {}", organisationId, e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    @Override
    public InputStream downloadFile(UUID organisationId, String objectKey) {
        try {
            String bucketName = getBucketName(organisationId);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error downloading file: {} for organisation: {}", objectKey, organisationId, e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }

    @Override
    public void deleteFile(UUID organisationId, String objectKey) {
        try {
            String bucketName = getBucketName(organisationId);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted file: {} from organisation bucket: {}", objectKey, bucketName);
        } catch (Exception e) {
            log.error("Error deleting file: {} for organisation: {}", objectKey, organisationId, e);
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    @Override
    public boolean fileExists(UUID organisationId, String objectKey) {
        try {
            String bucketName = getBucketName(organisationId);
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void createFolderStructure(UUID organisationId, String folderPath) {
        try {
            // Create an empty object with folder path + "/" to simulate folder
            String objectKey = folderPath.endsWith("/") ? folderPath + ".keep" : folderPath + "/.keep";
            uploadFile(organisationId, "", objectKey, new ByteArrayInputStream(new byte[0]), 0, "text/plain");
        } catch (Exception e) {
            log.error("Error creating folder structure: {} for organisation: {}", folderPath, organisationId, e);
            throw new RuntimeException("Failed to create folder structure", e);
        }
    }

    @Override
    public void deleteFolderStructure(UUID organisationId, String folderPath) {
        try {
            String bucketName = getBucketName(organisationId);
            String prefix = folderPath.endsWith("/") ? folderPath : folderPath + "/";

            // List all objects with this prefix
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            List<DeleteObject> objectsToDelete = new ArrayList<>();
            for (Result<Item> result : results) {
                objectsToDelete.add(new DeleteObject(result.get().objectName()));
            }

            if (!objectsToDelete.isEmpty()) {
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(bucketName)
                                .objects(objectsToDelete)
                                .build()
                );
            }

            log.info("Deleted folder structure: {} from organisation bucket: {}", folderPath, bucketName);
        } catch (Exception e) {
            log.error("Error deleting folder structure: {} for organisation: {}", folderPath, organisationId, e);
            throw new RuntimeException("Failed to delete folder structure", e);
        }
    }

    @Override
    public List<String> listFolderContents(UUID organisationId, String folderPath) {
        try {
            String bucketName = getBucketName(organisationId);
            String prefix = folderPath.isEmpty() ? "" : (folderPath.endsWith("/") ? folderPath : folderPath + "/");

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .build()
            );

            List<String> contents = new ArrayList<>();
            for (Result<Item> result : results) {
                contents.add(result.get().objectName());
            }

            return contents;
        } catch (Exception e) {
            log.error("Error listing folder contents: {} for organisation: {}", folderPath, organisationId, e);
            throw new RuntimeException("Failed to list folder contents", e);
        }
    }

    @Override
    public String generateObjectKey(String folderPath, String fileName) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return fileName;
        }

        String cleanFolderPath = folderPath.trim();
        if (cleanFolderPath.endsWith("/")) {
            return cleanFolderPath + fileName;
        } else {
            return cleanFolderPath + "/" + fileName;
        }
    }

    @Override
    public String getBucketName(UUID organisationId) {
        return minioConfig.getBucketPrefix() + organisationId.toString();
    }

    @Override
    public long getFileSize(UUID organisationId, String objectKey) {
        try {
            String bucketName = getBucketName(organisationId);
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return stat.size();
        } catch (Exception e) {
            log.error("Error getting file size: {} for organisation: {}", objectKey, organisationId, e);
            throw new RuntimeException("Failed to get file size", e);
        }
    }

    @Override
    public String getFileContentType(UUID organisationId, String objectKey) {
        try {
            String bucketName = getBucketName(organisationId);
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return stat.contentType();
        } catch (Exception e) {
            log.error("Error getting file content type: {} for organisation: {}", objectKey, organisationId, e);
            return "application/octet-stream";
        }
    }

    @Override
    public String generatePresignedDownloadUrl(UUID organisationId, String objectKey, int expirationInMinutes) {
        try {
            String bucketName = getBucketName(organisationId);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expirationInMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned download URL: {} for organisation: {}", objectKey, organisationId, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public String generatePresignedUploadUrl(UUID organisationId, String objectKey, int expirationInMinutes) {
        try {
            String bucketName = getBucketName(organisationId);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expirationInMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned upload URL: {} for organisation: {}", objectKey, organisationId, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public void renameFile(UUID organisationId, String oldKey, String newKey) {
        try {
            String bucketName = getBucketName(organisationId);

            // Copy to new location
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newKey)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(oldKey)
                                    .build())
                            .build()
            );

            // Delete old object
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(oldKey)
                            .build()
            );

            log.info("File renamed in organisation MinIO bucket: {} -> {}", oldKey, newKey);

        } catch (Exception e) {
            log.error("Failed to rename file in organisation MinIO bucket: {} -> {}", oldKey, newKey, e);
            throw new RuntimeException("Failed to rename file in storage", e);
        }
    }

    @Override
    public void copyFile(UUID organisationId, String sourceKey, String destinationKey) {
        try {
            String bucketName = getBucketName(organisationId);

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(destinationKey)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(sourceKey)
                                    .build())
                            .build()
            );

            log.info("File copied in organisation MinIO bucket: {} -> {}", sourceKey, destinationKey);

        } catch (Exception e) {
            log.error("Failed to copy file in organisation MinIO bucket: {} -> {}", sourceKey, destinationKey, e);
            throw new RuntimeException("Failed to copy file in storage", e);
        }
    }
}