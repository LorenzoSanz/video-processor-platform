package com.lorenzosanz.uploadservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.lorenzosanz.uploadservice.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.entity.ProcessingJob;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.repository.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "processing.max-attempts=1")
@Import(PostgresTestContainerConfiguration.class)
class ProcessingJobFailureMaxAttemptsIntegrationTest {

    @Autowired
    private ProcessingJobFailureService failureService;

    @Autowired
    private ProcessingJobClaimService claimService;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    void shouldFailJobWhenMaximumAttemptsIsReached() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "max-attempts-test-" + uploadId + ".mp4",
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

        try {
            ProcessingJob claimed = claimService.claim(
                    uploadId,
                    Duration.ofMinutes(2)
            ).orElseThrow();

            boolean accepted = failureService.fail(
                    claimed.getJobId(),
                    claimed.getClaimToken(),
                    "Temporary error, but no attempts remain",
                    true
            );

            assertTrue(accepted);

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            Upload persistedUpload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(1, persisted.getAttempts());
            assertEquals(ProcessingJobStatus.FAILED, persisted.getStatus());
            assertEquals(UploadStatus.FAILED, persistedUpload.getStatus());

        } finally {
            processingJobRepository.findByUploadId(uploadId)
                    .ifPresent(current ->
                            processingJobRepository.deleteById(current.getJobId())
                    );

            if (uploadRepository.existsById(uploadId)) {
                uploadRepository.deleteById(uploadId);
            }
        }
    }
}