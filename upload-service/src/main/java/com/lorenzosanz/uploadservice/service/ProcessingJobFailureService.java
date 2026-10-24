package com.lorenzosanz.uploadservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.entity.ProcessingJob;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.repository.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

@Service
public class ProcessingJobFailureService {

    private final ProcessingJobRepository processingJobRepository;
    private final UploadRepository uploadRepository;
    private final int maxAttempts;
    private final long initialBackoffSeconds;
    private final long maxBackoffSeconds;

    public ProcessingJobFailureService(
            ProcessingJobRepository processingJobRepository,
            UploadRepository uploadRepository,
            @Value("${processing.max-attempts:5}") int maxAttempts,
            @Value("${processing.initial-backoff-seconds:5}")
            long initialBackoffSeconds,
            @Value("${processing.max-backoff-seconds:300}")
            long maxBackoffSeconds) {

        if (maxAttempts < 1
                || initialBackoffSeconds < 1
                || maxBackoffSeconds < initialBackoffSeconds) {
            throw new IllegalArgumentException(
                    "Invalid processing retry configuration"
            );
        }

        this.processingJobRepository = processingJobRepository;
        this.uploadRepository = uploadRepository;
        this.maxAttempts = maxAttempts;
        this.initialBackoffSeconds = initialBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    @Transactional
    public boolean fail(
            UUID jobId,
            UUID claimToken,
            String errorMessage,
            boolean retryable) {

        if (jobId == null || claimToken == null) {
            throw new IllegalArgumentException(
                    "Job ID and claim token are required"
            );
        }

        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "Failure message is required"
            );
        }

        ProcessingJob job = processingJobRepository
                .findByJobIdForUpdate(jobId)
                .orElse(null);

        if (job == null) {
            return false;
        }

        Instant now = Instant.now();
        Instant retryAt = now.plus(
                calculateRetryDelay(job.getAttempts())
        );

        boolean updated = job.fail(
                claimToken,
                now,
                errorMessage,
                maxAttempts,
                retryAt,
                retryable
        );

        if (!updated) {
            return false;
        }

        Upload upload = uploadRepository.findById(job.getUploadId())
                .orElseThrow(() -> new IllegalStateException(
                        "Processing job references a missing upload"
                ));

        if (job.getStatus() == ProcessingJobStatus.FAILED) {
            upload.setStatus(UploadStatus.FAILED);
        } else {
            upload.setStatus(UploadStatus.PENDING);
        }

        return true;
    }

    private Duration calculateRetryDelay(int attempts) {
        long delaySeconds = initialBackoffSeconds;

        for (int i = 1;
                i < attempts && delaySeconds < maxBackoffSeconds;
                i++) {
            delaySeconds = Math.min(
                    delaySeconds * 2,
                    maxBackoffSeconds
            );
        }

        return Duration.ofSeconds(delaySeconds);
    }
}