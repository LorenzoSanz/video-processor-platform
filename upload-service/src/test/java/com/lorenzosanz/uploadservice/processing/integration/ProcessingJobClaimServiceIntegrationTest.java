package com.lorenzosanz.uploadservice.processing.integration;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimService;

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
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class ProcessingJobClaimServiceIntegrationTest {

    @Autowired
    private ProcessingJobClaimService claimService;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    void shouldClaimPendingJobAndSetLeaseMetadata() {
        UUID uploadId = createJob(Instant.now());

        try {
            Instant beforeClaim = Instant.now();

            ProcessingJobClaimResult.Claimed claimResult = assertInstanceOf(
                ProcessingJobClaimResult.Claimed.class,
                claimService.claim(
                        uploadId,
                        Duration.ofMinutes(2)
                )
            );

            Instant afterClaim = Instant.now();

            ProcessingJob claimed = claimResult.job();

            assertEquals(
                ProcessingJobStatus.PROCESSING,
                claimed.getStatus()
            );

            assertEquals(1, claimed.getAttempts());
            assertNotNull(claimed.getClaimToken());
            assertNotNull(claimed.getLeaseUntil());
            assertNotNull(claimed.getLastHeartbeatAt());
            assertNotNull(claimed.getStartedAt());

            assertFalse(claimed.getLeaseUntil().isBefore(
                    beforeClaim.plus(Duration.ofMinutes(2))
            ));
            assertFalse(claimed.getLeaseUntil().isAfter(
                    afterClaim.plus(Duration.ofMinutes(2))
            ));

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldNotClaimSameJobAgainWhileLeaseIsActive() {
        UUID uploadId = createJob(Instant.now());

        try {
            ProcessingJob firstClaim = assertInstanceOf(
                ProcessingJobClaimResult.Claimed.class,
                claimService.claim(
                        uploadId,
                        Duration.ofMinutes(2)
                )
            ).job();

            UUID originalToken = firstClaim.getClaimToken();

            ProcessingJobClaimResult secondClaim = claimService.claim(
                uploadId,
                Duration.ofMinutes(2)
            );

            assertInstanceOf(
                ProcessingJobClaimResult.LeaseActive.class,
                secondClaim
            );

            ProcessingJob persistedJob = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(ProcessingJobStatus.PROCESSING,
                    persistedJob.getStatus());
            assertEquals(1, persistedJob.getAttempts());
            assertEquals(originalToken, persistedJob.getClaimToken());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldNotClaimJobBeforeNextAttemptTime() {
        UUID uploadId = createJob(Instant.now().plus(Duration.ofMinutes(5)));

        try {
            ProcessingJobClaimResult result = claimService.claim(
                uploadId,
                Duration.ofMinutes(2)
            );

            assertInstanceOf(
                ProcessingJobClaimResult.RetryNotDue.class,
                result
            );

            ProcessingJob persistedJob = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(ProcessingJobStatus.PENDING,
                    persistedJob.getStatus());
            assertEquals(0, persistedJob.getAttempts());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldReturnNotFoundWhenJobDoesNotExist() {
        ProcessingJobClaimResult result = claimService.claim(
                UUID.randomUUID(),
                Duration.ofMinutes(2)
        );
    
        assertInstanceOf(
                ProcessingJobClaimResult.NotFound.class,
                result
        );
    }

    private UUID createJob(Instant jobCreatedAt) {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "claim-test-" + uploadId + ".mp4",
                "video/mp4",
                1024L,
                Instant.now()
        );

        uploadRepository.saveAndFlush(upload);

        ProcessingJob job = new ProcessingJob(
                UUID.randomUUID(),
                uploadId,
                jobCreatedAt
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