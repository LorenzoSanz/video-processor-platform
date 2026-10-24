package com.lorenzosanz.uploadservice.upload.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadLifecycleTest {

    @Test
    void shouldAssignStoragePathOnce() {
        Upload upload = createUpload(UploadStatus.PENDING);

        upload.assignStoragePath("2026/10/video.mp4");

        assertEquals(
                "2026/10/video.mp4",
                upload.getStoragePath()
        );
    }

    @Test
    void shouldRejectBlankStoragePath() {
        Upload upload = createUpload(UploadStatus.PENDING);

        assertThrows(
                IllegalArgumentException.class,
                () -> upload.assignStoragePath("  ")
        );
    }

    @Test
    void shouldRejectAssigningStoragePathTwice() {
        Upload upload = createUpload(UploadStatus.PENDING);

        upload.assignStoragePath("2026/10/video.mp4");

        assertThrows(
                IllegalStateException.class,
                () -> upload.assignStoragePath("2026/10/other.mp4")
        );
    }

    @Test
    void shouldTransitionFromPendingToProcessing() {
        Upload upload = createUpload(UploadStatus.PENDING);

        upload.markProcessing();

        assertEquals(UploadStatus.PROCESSING, upload.getStatus());
    }

    @Test
    void shouldAllowProcessingStateWhenLeaseIsRecovered() {
        Upload upload = createUpload(UploadStatus.PROCESSING);

        upload.markProcessing();

        assertEquals(UploadStatus.PROCESSING, upload.getStatus());
    }

    @Test
    void shouldTransitionFromProcessingToCompleted() {
        Upload upload = createUpload(UploadStatus.PROCESSING);

        upload.markCompleted();

        assertEquals(UploadStatus.COMPLETED, upload.getStatus());
    }

    @Test
    void shouldTransitionFromProcessingToPendingForRetry() {
        Upload upload = createUpload(UploadStatus.PROCESSING);

        upload.markPendingForRetry();

        assertEquals(UploadStatus.PENDING, upload.getStatus());
    }

    @Test
    void shouldTransitionFromProcessingToFailed() {
        Upload upload = createUpload(UploadStatus.PROCESSING);

        upload.markFailed();

        assertEquals(UploadStatus.FAILED, upload.getStatus());
    }

    @Test
    void shouldRejectCompletingUploadThatIsNotProcessing() {
        Upload upload = createUpload(UploadStatus.PENDING);

        assertThrows(
                IllegalStateException.class,
                upload::markCompleted
        );
    }

    @Test
    void shouldRejectRetryingUploadThatIsNotProcessing() {
        Upload upload = createUpload(UploadStatus.PENDING);

        assertThrows(
                IllegalStateException.class,
                upload::markPendingForRetry
        );
    }

    @Test
    void shouldRejectProcessingTerminalUpload() {
        Upload completed = createUpload(UploadStatus.COMPLETED);
        Upload failed = createUpload(UploadStatus.FAILED);

        assertThrows(
                IllegalStateException.class,
                completed::markProcessing
        );

        assertThrows(
                IllegalStateException.class,
                failed::markProcessing
        );
    }

    private Upload createUpload(UploadStatus status) {
        return new Upload(
                UUID.randomUUID(),
                "test-video.mp4",
                status,
                "video/mp4",
                1024L,
                Instant.now()
        );
    }
}