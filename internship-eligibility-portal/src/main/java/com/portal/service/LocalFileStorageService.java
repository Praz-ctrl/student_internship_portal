package com.portal.service;

import com.portal.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "txt", Set.of("text/plain")
    );

    private final Path root;
    private final Tika tika = new Tika();

    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory " + root, e);
        }
    }

    public static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Please choose a file to upload", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\"))) {
            throw new ApiException("Invalid file path in filename", HttpStatus.BAD_REQUEST);
        }

        String ext = extensionOf(originalFilename);
        if (!ALLOWED_MIME_TYPES.containsKey(ext)) {
            throw new ApiException("Unsupported file extension. Allowed: PDF, PNG, JPG, TXT", HttpStatus.BAD_REQUEST);
        }

        // Deep content-type sniffing via Apache Tika (magic bytes verification)
        try (InputStream in = file.getInputStream()) {
            String detectedMime = tika.detect(in, originalFilename);
            Set<String> validMimes = ALLOWED_MIME_TYPES.get(ext);
            if (validMimes == null || !validMimes.contains(detectedMime)) {
                log.warn("Mismatched or disallowed file content: extension={}, detectedMime={}", ext, detectedMime);
                throw new ApiException("File content does not match extension. Detected type: " + detectedMime, HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ApiException("Failed to inspect file content", HttpStatus.BAD_REQUEST);
        }

        String stored = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException("Could not store the file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return stored;
    }

    @Override
    public ResponseEntity<Resource> asResponse(String stored) {
        if (stored == null) throw new ApiException("No file attached", HttpStatus.NOT_FOUND);
        Path p = getPath(stored);
        if (!Files.exists(p)) {
            throw new ApiException("File not found", HttpStatus.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(p);
        MediaType type = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + stored + "\"")
                .body(resource);
    }

    @Override
    public Path getPath(String stored) {
        if (stored == null) return null;
        Path p = root.resolve(stored).normalize();
        if (!p.startsWith(root)) {
            throw new ApiException("Access denied to file outside upload directory", HttpStatus.FORBIDDEN);
        }
        return p;
    }

    @Override
    public void delete(String stored) {
        if (stored == null) return;
        try {
            Path p = getPath(stored);
            if (Files.exists(p)) {
                Files.delete(p);
            }
        } catch (IOException e) {
            log.error("Failed to delete stored file: {}", stored, e);
        }
    }
}
