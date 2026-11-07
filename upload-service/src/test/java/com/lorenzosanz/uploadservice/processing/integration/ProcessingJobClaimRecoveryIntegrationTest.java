
package com.lorenzosanz.uploadservice.processing.integration;

import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimResult;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimService;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = "processing.max-attempts=2")
@Import(PostgresTestContainerConfiguration.class)
class ProcessingJobClaimRecoveryIntegrationTest {

    @Autowired
    private ProcessingJobClaimService claimService;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    void shouldFailJobWhenMaximumAttemptsAreExhaustedAfterWorkerCrashes() {
        UUID uploadId = UUID.randomUUID();

        Instant firstClaimAt = Instant.now().minus(Duration.ofMinutes(10));
        Duration leaseDuration = Duration.ofMinutes(1);

        Upload upload = new Upload(
                uploadId,
                "crash-recovery-test-" + uploadId + ".mp4",
                "video/mp4",
                1024L,
                Instant.now()
        );

        uploadRepository.saveAndFlush(upload);

        ProcessingJob job = new ProcessingJob(
                UUID.randomUUID(),
                uploadId,
                firstClaimAt.minusSeconds(1)
        );

        // Simulate the first worker claiming the job and crashing.
        Instant firstLeaseUntil = firstClaimAt.plus(leaseDuration);

        job.claim(
                UUID.randomUUID(),
                firstClaimAt,
                firstLeaseUntil
        );

        // Simulate lease expiration and a second worker claiming the job.
        Instant secondClaimAt = firstLeaseUntil.plusSeconds(1);
        Instant secondLeaseUntil = secondClaimAt.plus(leaseDuration);

        job.claim(
                UUID.randomUUID(),
                secondClaimAt,
                secondLeaseUntil
        );

        processingJobRepository.saveAndFlush(job);

        try {
            // Both attempts have been consumed and the second lease expired.
            assertEquals(2, job.getAttempts());
            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    job.getStatus()
            );

            // A third worker must not receive another claim.
            ProcessingJobClaimResult result = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            );

            assertInstanceOf(
                    ProcessingJobClaimResult.TerminallyFailed.class,
                    result
            );

            ProcessingJob persistedJob = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(2, persistedJob.getAttempts());
            assertEquals(
                    ProcessingJobStatus.FAILED,
                    persistedJob.getStatus()
            );
            assertEquals(
                    "Maximum processing attempts reached",
                    persistedJob.getLastError()
            );
            assertNull(persistedJob.getClaimToken());
            assertNull(persistedJob.getLeaseUntil());

            // Further claims must remain terminal and must not increment attempts.
            ProcessingJobClaimResult subsequentResult = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            );

            assertInstanceOf(
                    ProcessingJobClaimResult.TerminallyFailed.class,
                    subsequentResult
            );

            ProcessingJob finalJob = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(2, finalJob.getAttempts());
            assertEquals(
                    ProcessingJobStatus.FAILED,
                    finalJob.getStatus()
            );

        } finally {
            processingJobRepository.findByUploadId(uploadId)
                    .ifPresent(current ->
                            processingJobRepository.deleteById(
                                    current.getJobId()
                            )
                    );

            uploadRepository.deleteById(uploadId);
        }
    }
}
