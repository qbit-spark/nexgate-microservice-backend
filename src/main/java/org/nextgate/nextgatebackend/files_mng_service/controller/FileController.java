package org.nextgate.nextgatebackend.files_mng_service.controller;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.files_mng_service.enums.FileDirectory;
import org.nextgate.nextgatebackend.files_mng_service.payload.FileResponse;
import org.nextgate.nextgatebackend.files_mng_service.payload.FileUploadResponse;
import org.nextgate.nextgatebackend.files_mng_service.service.FileService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final AccountRepo accountRepo;

    @PostMapping("/upload")
    public ResponseEntity<GlobeSuccessResponseBuilder> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("directory") FileDirectory directory) throws ItemNotFoundException {

        UUID accountId = getAuthenticatedAccount().getId();

        FileUploadResponse response = fileService.uploadFiles(accountId, directory, files);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Files uploaded successfully",
                response
        ));
    }

    @PostMapping("/upload-single")
    public ResponseEntity<GlobeSuccessResponseBuilder> uploadSingleFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("directory") FileDirectory directory) throws ItemNotFoundException {


        UUID accountId = getAuthenticatedAccount().getId();

        FileResponse response = fileService.uploadSingleFile(accountId, directory, file);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "File uploaded successfully",
                response
        ));
    }

    @GetMapping("/directory/{directory}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getUserFiles(
            @PathVariable FileDirectory directory) throws ItemNotFoundException {

        UUID accountId = getAuthenticatedAccount().getId();

        List<FileResponse> files = fileService.getUserFiles(accountId, directory);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Files retrieved successfully",
                files
        ));
    }

    @DeleteMapping("/{objectKey}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteFile(
            @PathVariable String objectKey) throws ItemNotFoundException {

        UUID accountId = getAuthenticatedAccount().getId();

        fileService.deleteFile(accountId, objectKey);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "File deleted successfully"
        ));
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}