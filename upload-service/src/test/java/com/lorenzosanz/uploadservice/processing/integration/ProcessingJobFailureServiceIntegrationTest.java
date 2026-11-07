package com.lorenzosanz.uploadservice.processing.integration;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimResult;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimService;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobFailureResult;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobFailureService;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
            ProcessingJob claimed = assertInstanceOf(
                    ProcessingJobClaimResult.Claimed.class,
                    claimService.claim(
                            uploadId,
                            Duration.ofMinutes(2)
                    )
            ).job();

            Instant beforeFailure = Instant.now();

            ProcessingJobFailureResult.RetryScheduled result =
                    assertInstanceOf(
                            ProcessingJobFailureResult.RetryScheduled.class,
                            failureService.fail(
                                    claimed.getJobId(),
                                    claimed.getClaimToken(),
                                    "Temporary processing error",
                                    true
                            )
                    );

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.PENDING,
                    persisted.getStatus()
            );
            assertEquals(1, persisted.getAttempts());
            assertEquals(
                    "Temporary processing error",
                    persisted.getLastError()
            );
            assertNull(persisted.getClaimToken());
            assertNull(persisted.getLeaseUntil());

            assertEquals(
                    result.nextAttemptAt(),
                    persisted.getNextAttemptAt()
            );

            assertTrue(
                    result.nextAttemptAt().isAfter(beforeFailure)
            );

            assertTrue(
                    result.nextAttemptAt()
                            .isBefore(beforeFailure.plusSeconds(15))
            );

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldMarkJobFailedForPermanentFailure() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = assertInstanceOf(
                    ProcessingJobClaimResult.Claimed.class,
                    claimService.claim(
                            uploadId,
                            Duration.ofMinutes(2)
                    )
            ).job();

            ProcessingJobFailureResult.TerminalFailure result =
                    assertInstanceOf(
                            ProcessingJobFailureResult.TerminalFailure.class,
                            failureService.fail(
                                    claimed.getJobId(),
                                    claimed.getClaimToken(),
                                    "Unsupported or corrupted video",
                                    false
                            )
                    );

            assertEquals(
                    ProcessingJobFailureResult.TerminalFailureReason.NON_RETRYABLE_FAILURE,
                    result.reason()
            );

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.FAILED,
                    persisted.getStatus()
            );
            assertEquals(1, persisted.getAttempts());
            assertEquals(
                    "Unsupported or corrupted video",
                    persisted.getLastError()
            );
            assertNull(persisted.getClaimToken());
            assertNull(persisted.getLeaseUntil());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRejectFailureWithWrongClaimToken() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = assertInstanceOf(
                    ProcessingJobClaimResult.Claimed.class,
                    claimService.claim(
                            uploadId,
                            Duration.ofMinutes(2)
                    )
            ).job();

            ProcessingJobFailureResult result = failureService.fail(
                    claimed.getJobId(),
                    UUID.randomUUID(),
                    "Temporary error",
                    true
            );

            assertInstanceOf(
                    ProcessingJobFailureResult.Rejected.class,
                    result
            );

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
    void shouldRejectFailureAfterLeaseExpires()
            throws InterruptedException {

        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = assertInstanceOf(
                    ProcessingJobClaimResult.Claimed.class,
                    claimService.claim(
                            uploadId,
                            Duration.ofMillis(200)
                    )
            ).job();

            Thread.sleep(500);

            ProcessingJobFailureResult result = failureService.fail(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    "Late failure report",
                    true
            );

            assertInstanceOf(
                    ProcessingJobFailureResult.Rejected.class,
                    result
            );

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
    void shouldReturnNotFoundWhenJobDoesNotExist() {
        ProcessingJobFailureResult result = failureService.fail(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Unknown job",
                true
        );

        assertInstanceOf(
                ProcessingJobFailureResult.NotFound.class,
                result
        );
    }

    private UUID createJob() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "failure-test-" + uploadId + ".mp4",
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