package com.lorenzosanz.uploadservice.upload.infrastructure.storage;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lorenzosanz.uploadservice.upload.config.StorageProperties;
import com.lorenzosanz.uploadservice.upload.exception.StorageException;

class LocalStorageServiceTest {

    private static final byte[] PNG_CONTENT = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

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
    void shouldStoreExactContentUsingBoundedCopy() throws IOException {
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-24T13:00:00Z");

        String storagePath = localStorageService.storeFile(
                uploadId,
                "test.png",
                createdAt,
                PNG_CONTENT.length,
                new ByteArrayInputStream(PNG_CONTENT)
        );

        assertEquals("2026/08/" + uploadId + ".png", storagePath);
        Path storedFile = tempDir.resolve(storagePath);
        assertTrue(Files.isRegularFile(storedFile));
        assertEquals(PNG_CONTENT.length, Files.size(storedFile));
        assertArrayEquals(PNG_CONTENT, Files.readAllBytes(storedFile));
    }

    @Test
    void shouldBuildStoragePathUsingUtcYearMonthAndUploadId() throws IOException {
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = LocalDateTime.of(2026, 8, 24, 13, 0)
                .toInstant(ZoneOffset.UTC);

        String storagePath = localStorageService.storeFile(
                uploadId,
                "test.png",
                createdAt,
                PNG_CONTENT.length,
                new ByteArrayInputStream(PNG_CONTENT)
        );

        assertEquals("2026/08/" + uploadId + ".png", storagePath);
    }

    @Test
    void shouldDeleteStoredFile() throws IOException {
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-24T13:00:00Z");

        String storagePath = localStorageService.storeFile(
                uploadId,
                "test.png",
                createdAt,
                PNG_CONTENT.length,
                new ByteArrayInputStream(PNG_CONTENT)
        );
        Path storedFile = tempDir.resolve(storagePath);

        assertTrue(Files.exists(storedFile));
        localStorageService.deleteFile(storagePath);
        assertFalse(Files.exists(storedFile));
    }

    @Test
    void shouldCreateDirectoriesWhenStoringFile() throws IOException {
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-24T13:00:00Z");

        String storagePath = localStorageService.storeFile(
                uploadId,
                "test.png",
                createdAt,
                PNG_CONTENT.length,
                new ByteArrayInputStream(PNG_CONTENT)
        );

        Path expectedDirectory = tempDir.resolve("2026/08");
        assertTrue(Files.isDirectory(expectedDirectory));
        assertTrue(Files.isRegularFile(tempDir.resolve(storagePath)));
    }

    @Test
    void shouldNotOverwriteExistingFile() throws IOException {
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-24T13:00:00Z");
        byte[] originalContent = PNG_CONTENT.clone();
        byte[] replacementContent = {1, 2, 3, 4};

        String storagePath = localStorageService.storeFile(
                uploadId,
                "test.png",
                createdAt,
                originalContent.length,
                new ByteArrayInputStream(originalContent)
        );
        Path storedFile = tempDir.resolve(storagePath);

        assertThrows(
                StorageException.class,
                () -> localStorageService.storeFile(
                        uploadId,
                        "test.png",
                        createdAt,
                        replacementContent.length,
                        new ByteArrayInputStream(replacementContent)
                )
        );

        assertArrayEquals(originalContent, Files.readAllBytes(storedFile));
    }

    @Test
    void shouldDeletePartialFileWhenInputStreamFails() {
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-24T13:00:00Z");
        byte[] firstChunk = {1, 2, 3, 4};
        Path expectedFile = tempDir.resolve(
                "2026/08/" + uploadId + ".png"
        );

        InputStream failingStream = new InputStream() {
            private boolean firstRead = true;
            private int offset;

            @Override
            public int read() throws IOException {
                if (offset < firstChunk.length) {
                    return firstChunk[offset++] & 0xff;
                }
                throw new IOException("Simulated stream failure");
            }

            @Override
            public int read(byte[] buffer, int off, int len) throws IOException {
                if (!firstRead) {
                    throw new IOException("Simulated stream failure");
                }
                firstRead = false;
                int count = Math.min(len, firstChunk.length);
                System.arraycopy(firstChunk, 0, buffer, off, count);
                offset = count;
                return count;
            }
        };

        assertThrows(
                StorageException.class,
                () -> localStorageService.storeFile(
                        uploadId,
                        "partial.png",
                        createdAt,
                        8,
                        failingStream
                )
        );

        assertFalse(Files.exists(expectedFile));
    }

    @Test
    void shouldDeletePartialFileWhenStreamSizeDoesNotMatchExpectedSize() {
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-24T13:00:00Z");
        Path expectedFile = tempDir.resolve(
                "2026/08/" + uploadId + ".png"
        );

        assertThrows(
                StorageException.class,
                () -> localStorageService.storeFile(
                        uploadId,
                        "test.png",
                        createdAt,
                        PNG_CONTENT.length + 1L,
                        new ByteArrayInputStream(PNG_CONTENT)
                )
        );

        assertFalse(Files.exists(expectedFile));
    }

    @Test
    void shouldRejectStoragePathOutsideRoot() {
        assertThrows(
                StorageException.class,
                () -> localStorageService.deleteFile("../outside.txt")
        );
    }

    @Test
    void shouldReadStoredFileAsStream() throws IOException {
    UUID uploadId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-08-24T13:00:00Z");

    byte[] content = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    String storagePath = localStorageService.storeFile(
            uploadId,
            "test.png",
            createdAt,
            content.length,
            new java.io.ByteArrayInputStream(content)
    );

    try (InputStream inputStream =
                 localStorageService.openForRead(storagePath)) {

        assertArrayEquals(content, inputStream.readAllBytes());
    }
}

    @Test
    void shouldRejectPathOutsideStorageRoot() {
        assertThrows(
            StorageException.class,
            () -> localStorageService.openForRead("../../outside.png")
        );
}

    @Test
    void shouldFailWhenStoredFileDoesNotExist() {
        assertThrows(
            StorageException.class,
            () -> localStorageService.openForRead(
                    "2026/08/nonexistent.png"
            )
        );
}
}
