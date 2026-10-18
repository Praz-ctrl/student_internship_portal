package com.portal.service;

import com.portal.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString());
    }

    @Test
    void store_storesValidTextFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "Candidate resume text content".getBytes()
        );

        String stored = storageService.store(file);
        assertNotNull(stored);
        assertTrue(stored.endsWith(".txt"));
    }

    @Test
    void store_rejectsEmptyOrDisallowedExtension() {
        MockMultipartFile empty = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        assertThrows(ApiException.class, () -> storageService.store(empty));

        MockMultipartFile exe = new MockMultipartFile("file", "malware.exe", "application/x-dosexec", "MZ".getBytes());
        assertThrows(ApiException.class, () -> storageService.store(exe));
    }

    @Test
    void store_rejectsPathTraversal() {
        MockMultipartFile traversal = new MockMultipartFile(
                "file", "../../etc/passwd.txt", "text/plain", "test content".getBytes()
        );
        assertThrows(ApiException.class, () -> storageService.store(traversal));
    }
}
