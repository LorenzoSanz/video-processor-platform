package com.lorenzosanz.uploadservice.processing.integration;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimService;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobCompletionService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.upload.domain.UploadStatus;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class ProcessingJobCompletionServiceIntegrationTest {

    @Autowired
    private ProcessingJobCompletionService completionService;

    @Autowired
    private ProcessingJobClaimService claimService;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    void shouldCompleteJobAndUploadForValidClaim() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            ).orElseThrow();

            String outputPath = "processed/" + uploadId + ".mp4";

            boolean completed = completionService.complete(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    outputPath
            );

            assertTrue(completed);

            ProcessingJob persistedJob = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            Upload persistedUpload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.COMPLETED,
                    persistedJob.getStatus()
            );
            assertEquals(outputPath, persistedJob.getOutputPath());
            assertNotNull(persistedJob.getCompletedAt());
            assertNull(persistedJob.getClaimToken());
            assertNull(persistedJob.getLeaseUntil());

            assertEquals(
                    UploadStatus.COMPLETED,
                    persistedUpload.getStatus()
            );

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRejectCompletionWithWrongClaimToken() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            ).orElseThrow();

            boolean completed = completionService.complete(
                    claimed.getJobId(),
                    UUID.randomUUID(),
                    "processed/" + uploadId + ".mp4"
            );

            assertFalse(completed);

            ProcessingJob persistedJob = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            Upload persistedUpload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    persistedJob.getStatus()
            );
            assertNull(persistedJob.getCompletedAt());
            assertNotNull(persistedJob.getClaimToken());
            assertEquals(
                    UploadStatus.PROCESSING,
                    persistedUpload.getStatus()
            );

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRejectCompletionAfterLeaseExpires() throws InterruptedException {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMillis(200)
            ).orElseThrow();

            Thread.sleep(500);

            boolean completed = completionService.complete(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    "processed/" + uploadId + ".mp4"
            );

            assertFalse(completed);

            ProcessingJob persistedJob = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            Upload persistedUpload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    persistedJob.getStatus()
            );
            assertNull(persistedJob.getCompletedAt());
            assertEquals(
                    UploadStatus.PROCESSING,
                    persistedUpload.getStatus()
            );

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldReturnFalseWhenJobDoesNotExist() {
        boolean completed = completionService.complete(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "processed/nonexistent.mp4"
        );

        assertFalse(completed);
    }

    private UUID createJob() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "completion-test-" + uploadId + ".mp4",
                UploadStatus.PENDING,
                "video/mp4",
                1024L,
                Instant.now()
        );

        uploadRepository.saveAndFlush(upload);

        ProcessingJob job = new ProcessingJob(
                UUID.randomUUID(),
                uploadId,
                Instant.now()
        );

        processingJobRepository.saveAndFlush(job);

        return uploadId;
    }

    private void cleanUp(UUID uploadId) {
        processingJobRepository.findByUploadId(uploadId)
                .ifPresent(job ->
                        processingJobRepository.deleteById(job.getJobId())
                );

        if (uploadRepository.existsById(uploadId)) {
            uploadRepository.deleteById(uploadId);
        }
    }
}