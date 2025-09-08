package org.nextgate.nextgatebackend.files_mng_service.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private List<FileResponse> uploadedFiles;
    private int totalFiles;
    private int successfulUploads;
    private int failedUploads;
    private long totalSize;
    private String totalSizeFormatted; // "15.2 MB"
    private LocalDateTime uploadedAt;
    private String message;
    private List<String> errors; // Any upload errors
}
