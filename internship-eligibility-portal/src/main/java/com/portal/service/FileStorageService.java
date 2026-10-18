package com.portal.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * Storage abstraction for user uploads (marksheets, resumes, certificates).
 * Implemented currently by LocalFileStorageService, easily pluggable with S3StorageService.
 */
public interface FileStorageService {

    String store(MultipartFile file);

    ResponseEntity<Resource> asResponse(String stored);

    Path getPath(String stored);

    void delete(String stored);

    static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
