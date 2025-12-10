package org.nextgate.nextgatebackend.files_mng_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.files_mng_service.enums.FileDirectory;
import org.nextgate.nextgatebackend.files_mng_service.payload.FileResponse;
import org.nextgate.nextgatebackend.files_mng_service.payload.FileUploadResponse;
import org.nextgate.nextgatebackend.files_mng_service.service.FileService;
import org.nextgate.nextgatebackend.minio_service.service.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final MinioService minioService;
    private final BlurHashServiceImpl blurHashService;

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB

    @Value("${files-server-url}")
    private String files_server_url;

    // File type mappings
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm", "video/mkv");
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "application/vnd.ms-excel");
    private static final Set<String> AUDIO_TYPES = Set.of("audio/mpeg", "audio/wav", "audio/mp3", "audio/ogg", "audio/aac");

    @Override
    public FileUploadResponse uploadFiles(UUID accountId, FileDirectory directory, MultipartFile[] files) {
        List<FileResponse> uploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long totalSize = 0;
        int successfulUploads = 0;
        int failedUploads = 0;

        for (MultipartFile file : files) {
            try {
                validateFile(file);
                FileResponse fileResponse = uploadSingleFile(accountId, directory, file); // ✅ Already has BlurHash
                uploadedFiles.add(fileResponse);
                totalSize += file.getSize();
                successfulUploads++;
            } catch (Exception e) {
                String errorMsg = "Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage();
                errors.add(errorMsg);
                failedUploads++;
                log.error(errorMsg, e);
            }
        }

        String message = successfulUploads > 0
                ? successfulUploads + " files uploaded successfully"
                : "No files were uploaded";

        if (failedUploads > 0) {
            message += ", " + failedUploads + " failed";
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setUploadedFiles(uploadedFiles);
        response.setTotalFiles(files.length);
        response.setSuccessfulUploads(successfulUploads);
        response.setFailedUploads(failedUploads);
        response.setTotalSize(totalSize);
        response.setTotalSizeFormatted(formatFileSize(totalSize));
        response.setUploadedAt(LocalDateTime.now());
        response.setMessage(message);
        response.setErrors(errors.isEmpty() ? null : errors);

        return response;
    }
    @Override
    public FileResponse uploadSingleFile(UUID accountId, FileDirectory directory, MultipartFile file) {
        validateFile(file);

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType();

            boolean isImage = IMAGE_TYPES.contains(contentType);
            boolean isVideo = VIDEO_TYPES.contains(contentType);

            // Parallel tasks
            CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(() ->
                    minioService.uploadFile(accountId, directory.getPath(), uniqueFilename, file)
            );

            CompletableFuture<String> blurHashFuture = CompletableFuture.supplyAsync(() -> {
                if (isImage) {
                    return blurHashService.generateBlurHash(fileBytes);
                }
                return null;
            });

            CompletableFuture<String> checksumFuture = CompletableFuture.supplyAsync(() ->
                    generateChecksum(fileBytes)
            );

            CompletableFuture<int[]> dimensionsFuture = CompletableFuture.supplyAsync(() -> {
                if (isImage) {
                    return getImageDimensions(fileBytes);
                }
                return null;
            });

            // Wait for all
            CompletableFuture.allOf(uploadFuture, blurHashFuture, checksumFuture, dimensionsFuture).join();

            String objectKey = uploadFuture.get();
            String blurHash = blurHashFuture.get();
            String checksum = checksumFuture.get();
            int[] dimensions = dimensionsFuture.get();

            String permanentUrl = generatePublicUrl(accountId, objectKey);

            // Build response
            FileResponse response = new FileResponse();
            response.setFileName(uniqueFilename);
            response.setOriginalFileName(originalFilename);
            response.setObjectKey(objectKey);
            response.setDirectory(directory);
            response.setContentType(contentType);
            response.setFileSize(file.getSize());
            response.setFileSizeFormatted(formatFileSize(file.getSize()));
            response.setPermanentUrl(permanentUrl);
            response.setFileExtension(fileExtension);
            response.setFileType(determineFileType(contentType));
            response.setIsImage(isImage);
            response.setIsVideo(isVideo);
            response.setIsDocument(DOCUMENT_TYPES.contains(contentType));
            response.setIsAudio(AUDIO_TYPES.contains(contentType));
            response.setChecksum(checksum);
            response.setBlurHash(blurHash);
            response.setUploadedAt(LocalDateTime.now());
            response.setUploadedBy(accountId.toString());
            response.setIsPublic(true);

            if (dimensions != null) {
                response.setWidth(dimensions[0]);
                response.setHeight(dimensions[1]);
                response.setDimensions(dimensions[0] + "x" + dimensions[1]);
            }

            if (isImage) {
                response.setThumbnailUrl(permanentUrl);
            }

            log.info("File uploaded successfully: {} for account: {}", originalFilename, accountId);
            return response;

        } catch (Exception e) {
            log.error("Failed to upload file: {} for account: {}", file.getOriginalFilename(), accountId, e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public List<FileResponse> getUserFiles(UUID accountId, FileDirectory directory) {
        try {
            List<String> objectKeys = minioService.listFolderContents(accountId, directory.getPath());
            List<FileResponse> fileResponses = new ArrayList<>();

            for (String objectKey : objectKeys) {
                if (objectKey.endsWith("/.keep") || objectKey.endsWith("/")) {
                    continue;
                }

                try {
                    FileResponse fileResponse = getFileDetails(accountId, objectKey, directory);
                    fileResponses.add(fileResponse);

                } catch (Exception e) {
                    log.warn("Failed to get details for file: {}", objectKey, e);
                }
            }

            return fileResponses;

        } catch (Exception e) {
            log.error("Failed to list files for account: {} in directory: {}", accountId, directory, e);
            throw new RuntimeException("Failed to retrieve files: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(UUID accountId, String objectKey) {
        try {
            minioService.deleteFile(accountId, objectKey);
            log.info("File deleted successfully: {} for account: {}", objectKey, accountId);

        } catch (Exception e) {
            log.error("Failed to delete file: {} for account: {}", objectKey, accountId, e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    private FileResponse createFileResponse(MultipartFile file, String originalFilename, String uniqueFilename,
                                            String objectKey, FileDirectory directory, String permanentUrl, UUID accountId) throws Exception {

        FileResponse response = new FileResponse();

        // Basic info
        response.setFileName(uniqueFilename);
        response.setOriginalFileName(originalFilename);
        response.setObjectKey(objectKey);
        response.setDirectory(directory);
        response.setContentType(file.getContentType());
        response.setFileSize(file.getSize());
        response.setFileSizeFormatted(formatFileSize(file.getSize()));
        response.setPermanentUrl(permanentUrl);

        // File type info
        String extension = getFileExtension(originalFilename);
        response.setFileExtension(extension);
        response.setFileType(determineFileType(file.getContentType()));
        response.setIsImage(IMAGE_TYPES.contains(file.getContentType()));
        response.setIsVideo(VIDEO_TYPES.contains(file.getContentType()));
        response.setIsDocument(DOCUMENT_TYPES.contains(file.getContentType()));
        response.setIsAudio(AUDIO_TYPES.contains(file.getContentType()));

        // Generate checksum
        response.setChecksum(generateChecksum(file.getBytes()));

        // Metadata
        response.setUploadedAt(LocalDateTime.now());
        response.setUploadedBy(accountId.toString());
        response.setIsPublic(true); // All files are public in ecommerce

        // Media specific info
        if (response.getIsImage()) {
            setImageDimensions(response, file.getBytes());
        }

        // Generate thumbnail URL if it's an image
        if (response.getIsImage()) {
            response.setThumbnailUrl(permanentUrl); // Same as permanent URL for now
        }

        return response;
    }

    private FileResponse getFileDetails(UUID accountId, String objectKey, FileDirectory directory) {
        try {
            String fileName = extractFileName(objectKey);
            String contentType = minioService.getFileContentType(accountId, objectKey);
            long fileSize = minioService.getFileSize(accountId, objectKey);

            // Generate public URL for existing files
            String permanentUrl = generatePublicUrl(accountId, objectKey);

            FileResponse response = new FileResponse();
            response.setFileName(fileName);
            response.setOriginalFileName(fileName);
            response.setObjectKey(objectKey);
            response.setDirectory(directory);
            response.setContentType(contentType);
            response.setFileSize(fileSize);
            response.setFileSizeFormatted(formatFileSize(fileSize));
            response.setPermanentUrl(permanentUrl);

            // File type info
            String extension = getFileExtension(fileName);
            response.setFileExtension(extension);
            response.setFileType(determineFileType(contentType));
            response.setIsImage(IMAGE_TYPES.contains(contentType));
            response.setIsVideo(VIDEO_TYPES.contains(contentType));
            response.setIsDocument(DOCUMENT_TYPES.contains(contentType));
            response.setIsAudio(AUDIO_TYPES.contains(contentType));

            response.setUploadedBy(accountId.toString());
            response.setIsPublic(true); // All files are public

            if (response.getIsImage()) {
                response.setThumbnailUrl(permanentUrl);
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get file details: " + e.getMessage());
        }
    }

    private void setImageDimensions(FileResponse response, byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                response.setWidth(image.getWidth());
                response.setHeight(image.getHeight());
                response.setDimensions(image.getWidth() + "x" + image.getHeight());
            }
        } catch (IOException e) {
            log.warn("Failed to read image dimensions", e);
        }
    }

    private String generateChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String determineFileType(String contentType) {
        if (contentType == null) return "OTHER";
        if (IMAGE_TYPES.contains(contentType)) return "IMAGE";
        if (VIDEO_TYPES.contains(contentType)) return "VIDEO";
        if (DOCUMENT_TYPES.contains(contentType)) return "DOCUMENT";
        if (AUDIO_TYPES.contains(contentType)) return "AUDIO";
        return "OTHER";
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum limit of 25MB");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new RuntimeException("File name is required");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String extractFileName(String objectKey) {
        if (objectKey.contains("/")) {
            return objectKey.substring(objectKey.lastIndexOf("/") + 1);
        }
        return objectKey;
    }

    private String generatePublicUrl(UUID accountId, String objectKey) {
        // Generate direct MinIO URL without expiration - all files are public
        String bucketName = minioService.getBucketName(accountId);
        return String.format(files_server_url+"/%s/%s", bucketName, objectKey);
    }

    private int[] getImageDimensions(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                return new int[]{image.getWidth(), image.getHeight()};
            }
        } catch (IOException e) {
            log.warn("Failed to read image dimensions", e);
        }
        return null;
    }
}