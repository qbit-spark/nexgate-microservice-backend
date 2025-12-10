package org.nextgate.nextgatebackend.files_mng_service.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.files_mng_service.enums.FileDirectory;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    // Basic file info
    private String fileName;
    private String originalFileName;
    private String objectKey;
    private FileDirectory directory;
    private String contentType;
    private Long fileSize;
    private String fileSizeFormatted; // "2.5 MB", "1.2 GB"

    // URLs
    private String permanentUrl;
    private String thumbnailUrl; // For images/videos
    private String blurHash;


    // File type info
    private String fileExtension;
    private String fileType; // IMAGE, VIDEO, DOCUMENT, AUDIO, OTHER
    private Boolean isImage;
    private Boolean isVideo;
    private Boolean isDocument;
    private Boolean isAudio;

    // Media specific info (for images/videos)
    private Integer width;
    private Integer height;
    private String dimensions; // "1920x1080"
    private Long duration; // Video duration in seconds
    private String durationFormatted; // "2:35" for videos

    // File metadata
    private String checksum; // MD5 hash
    private LocalDateTime uploadedAt;
    private String uploadedBy; // Account ID or username

    // Additional properties
    private Boolean isPublic;
    private String description;
    private String[] tags;
}