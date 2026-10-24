package com.lorenzosanz.uploadservice.upload.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadStoragePathTest {

    @Test
    void shouldAssignStoragePath() {
        Upload upload = createUpload();

        upload.assignStoragePath("2026/10/video.mp4");

        assertEquals(
                "2026/10/video.mp4",
                upload.getStoragePath()
        );
    }

    @Test
    void shouldRejectBlankStoragePath() {
        Upload upload = createUpload();

        assertThrows(
                IllegalArgumentException.class,
                () -> upload.assignStoragePath("  ")
        );
    }

    @Test
    void shouldRejectAssigningStoragePathTwice() {
        Upload upload = createUpload();

        upload.assignStoragePath("2026/10/video.mp4");

        assertThrows(
                IllegalStateException.class,
                () -> upload.assignStoragePath("2026/10/other.mp4")
        );
    }

    private Upload createUpload() {
        return new Upload(
                UUID.randomUUID(),
                "test-video.mp4",
                "video/mp4",
                1024L,
                Instant.now()
        );
    }
}