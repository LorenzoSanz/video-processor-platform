package com.lorenzosanz.uploadservice.processing.integration;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimService;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobHeartbeatService;

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
import com.lorenzosanz.uploadservice.upload.domain.UploadStatus;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class ProcessingJobHeartbeatServiceIntegrationTest {

    @Autowired
    private ProcessingJobHeartbeatService heartbeatService;

    @Autowired
    private ProcessingJobClaimService claimService;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    void shouldRenewLeaseForValidClaim() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(1)
            ).orElseThrow();

            UUID jobId = claimed.getJobId();
            UUID claimToken = claimed.getClaimToken();

            Instant originalLeaseUntil = claimed.getLeaseUntil();
            Instant originalHeartbeat = claimed.getLastHeartbeatAt();

            boolean renewed = heartbeatService.heartbeat(
                    jobId,
                    claimToken,
                    Duration.ofMinutes(5)
            );

            assertTrue(renewed);

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    claimed.getClaimToken(),
                    persisted.getClaimToken()
            );

            assertTrue(
                    persisted.getLeaseUntil().isAfter(originalLeaseUntil)
            );

            assertFalse(
                    persisted.getLastHeartbeatAt().isBefore(originalHeartbeat)
            );

            assertEquals(1, persisted.getAttempts());
            assertNotNull(persisted.getLastHeartbeatAt());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRejectHeartbeatWithWrongClaimToken() {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            ).orElseThrow();

            ProcessingJob before = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            boolean renewed = heartbeatService.heartbeat(
                    claimed.getJobId(),
                    UUID.randomUUID(),
                    Duration.ofMinutes(5)
            );

            assertFalse(renewed);

            ProcessingJob after = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(before.getClaimToken(), after.getClaimToken());
            assertEquals(before.getLeaseUntil(), after.getLeaseUntil());
            assertEquals(
                    before.getLastHeartbeatAt(),
                    after.getLastHeartbeatAt()
            );
            assertEquals(before.getAttempts(), after.getAttempts());

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRejectHeartbeatAfterLeaseExpires() throws InterruptedException {
        UUID uploadId = createJob();

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMillis(100)
            ).orElseThrow();

            // Esperamos a que venza el lease que hemos creado para el test.
            Thread.sleep(250);

            boolean renewed = heartbeatService.heartbeat(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    Duration.ofMinutes(1)
            );

            assertFalse(renewed);

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    claimed.getClaimToken(),
                    persisted.getClaimToken()
            );
            assertTrue(
                    !persisted.getLeaseUntil().isAfter(Instant.now())
            );

        } finally {
            cleanUp(uploadId);
        }
    }

    @Test
    void shouldReturnFalseWhenJobDoesNotExist() {
        boolean renewed = heartbeatService.heartbeat(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Duration.ofMinutes(1)
        );

        assertFalse(renewed);
    }

    private UUID createJob() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "heartbeat-test-" + uploadId + ".mp4",
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