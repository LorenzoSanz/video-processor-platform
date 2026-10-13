package com.lorenzosanz.uploadservice.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.lorenzosanz.uploadservice.config.StorageProperties;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService localStorageService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setRootPath(tempDir.toString());

        localStorageService = new LocalStorageService(storageProperties);
    }

    @Test
    void shouldStoreFile() throws IOException {

        UUID uploadId = UUID.randomUUID();

        LocalDateTime ldt = LocalDateTime.of(2026, 8, 24, 13, 0);
        Instant instant = ldt.toInstant(ZoneOffset.UTC);

        Upload upload = new Upload(
            uploadId,
            "test.png",
            UploadStatus.PENDING,
            "image/png",
            8L,
            instant // Pasamos directamente la variable ya procesada
        );

        byte[] content = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        };

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            content
        );

        localStorageService.storeFile(upload, file);

        assertTrue(upload.getStoragePath() != null);

        Path storedFile = tempDir.resolve(upload.getStoragePath());

        assertTrue(Files.exists(storedFile));
        assertEquals(content.length, Files.size(storedFile));
    }

    @Test
    void shouldBuildStoragePathUsingYearMonthAndUploadId() {

        UUID uploadId = UUID.randomUUID();

        // Si tu clase Upload usa Instant, aquí también debes cambiar LocalDateTime por Instant
        Instant instant = LocalDateTime.of(2026, 8, 24, 13, 0).toInstant(ZoneOffset.UTC);

        Upload upload = new Upload(
            uploadId,
            "test.png",
            UploadStatus.PENDING,
            "image/png",
            8L,
            instant
        );

        byte[] content = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        };

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            content
        );

        localStorageService.storeFile(upload, file);

        assertEquals(
            "2026/08/" + uploadId + ".png",
            upload.getStoragePath()
        );
    }

    @Test
    void shouldDeleteFile() throws IOException {

        UUID uploadId = UUID.randomUUID();

        Instant instant = LocalDateTime.of(2026, 8, 24, 13, 0).toInstant(ZoneOffset.UTC);

        Upload upload = new Upload(
            uploadId,
            "test.png",
            UploadStatus.PENDING,
            "image/png",
            8L,
            instant
        );

        byte[] content = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        };

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            content
        );

        localStorageService.storeFile(upload, file);

        Path storedFile = tempDir.resolve(upload.getStoragePath());

        assertTrue(Files.exists(storedFile));

        localStorageService.deleteFile(upload.getStoragePath());

        assertFalse(Files.exists(storedFile));
    }

    @Test
    void shouldCreateDirectoriesWhenStoringFile() {

        // Ajustado a Instant
        Instant instant = LocalDateTime.of(2026, 8, 24, 13, 0).toInstant(ZoneOffset.UTC);

        Upload upload = new Upload(
            UUID.randomUUID(),
            "test.png",
            UploadStatus.PENDING,
            "image/png",
            8L,
            instant
        );

        byte[] content = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        };

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            content
        );

        localStorageService.storeFile(upload, file);

        Path expectedDirectory = tempDir.resolve("2026/08");

        assertTrue(Files.exists(expectedDirectory));
        assertTrue(Files.isDirectory(expectedDirectory));
    }
}
