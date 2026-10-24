package com.lorenzosanz.uploadservice.processing.integration;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimService;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobFailureService;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class ProcessingJobFailureServiceIntegrationTest {

    @Autowired
    private ProcessingJobFailureService failureService;

    @Autowired
    private ProcessingJobClaimService claimService;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    void shouldScheduleRetryForRetryableFailure() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            ).orElseThrow();

            Instant beforeFailure = Instant.now();

            boolean accepted = failureService.fail(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    "Temporary processing error",
                    true
            );

            assertTrue(accepted);

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(ProcessingJobStatus.PENDING, persisted.getStatus());
            assertEquals(1, persisted.getAttempts());
            assertEquals(
                    "Temporary processing error",
                    persisted.getLastError()
            );
            assertNull(persisted.getClaimToken());
            assertNull(persisted.getLeaseUntil());

            assertTrue(persisted.getNextAttemptAt().isAfter(beforeFailure));
            assertTrue(
                    persisted.getNextAttemptAt()
                            .isBefore(beforeFailure.plusSeconds(15))
            );

            assertEquals(UploadStatus.PENDING, upload.getStatus());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldMarkJobAndUploadFailedForPermanentFailure() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            ).orElseThrow();

            boolean accepted = failureService.fail(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    "Unsupported or corrupted video",
                    false
            );

            assertTrue(accepted);

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(ProcessingJobStatus.FAILED, persisted.getStatus());
            assertEquals(1, persisted.getAttempts());
            assertEquals(
                    "Unsupported or corrupted video",
                    persisted.getLastError()
            );
            assertNull(persisted.getClaimToken());
            assertNull(persisted.getLeaseUntil());

            assertEquals(UploadStatus.FAILED, upload.getStatus());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRejectFailureWithWrongClaimToken() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            ).orElseThrow();

            boolean accepted = failureService.fail(
                    claimed.getJobId(),
                    UUID.randomUUID(),
                    "Temporary error",
                    true
            );

            assertFalse(accepted);

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    persisted.getStatus()
            );
            assertNull(persisted.getLastError());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRejectFailureAfterLeaseExpires() throws InterruptedException {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMillis(200)
            ).orElseThrow();

            Thread.sleep(500);

            boolean accepted = failureService.fail(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    "Late failure report",
                    true
            );

            assertFalse(accepted);

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    persisted.getStatus()
            );
            assertNull(persisted.getLastError());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldReturnFalseWhenJobDoesNotExist() {
        boolean accepted = failureService.fail(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Unknown job",
                true
        );

        assertFalse(accepted);
    }

    private UUID createJob() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "failure-test-" + uploadId + ".mp4",
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