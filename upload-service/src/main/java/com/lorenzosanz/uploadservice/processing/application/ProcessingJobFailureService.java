
package com.lorenzosanz.uploadservice.processing.application;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;

@Service
public class ProcessingJobFailureService {

    private final ProcessingJobRepository processingJobRepository;
    private final int maxAttempts;
    private final long initialBackoffSeconds;
    private final long maxBackoffSeconds;

    public ProcessingJobFailureService(
            ProcessingJobRepository processingJobRepository,
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
        this.maxAttempts = maxAttempts;
        this.initialBackoffSeconds = initialBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    @Transactional
    public ProcessingJobFailureResult fail(
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
            return new ProcessingJobFailureResult.NotFound();
        }

        Instant now = Instant.now();

        Instant retryAt = now.plus(
                calculateRetryDelay(job.getAttempts())
        );

        boolean accepted = job.fail(
                claimToken,
                now,
                errorMessage,
                maxAttempts,
                retryAt,
                retryable
        );

        if (!accepted) {
            return new ProcessingJobFailureResult.Rejected();
        }

        return switch (job.getStatus()) {
            case PENDING ->
                    new ProcessingJobFailureResult.RetryScheduled(
                            job.getNextAttemptAt()
                    );

            case FAILED ->
                    new ProcessingJobFailureResult.TerminalFailure(
                            retryable
                                    ? ProcessingJobFailureResult.TerminalFailureReason.MAX_ATTEMPTS_REACHED
                                    : ProcessingJobFailureResult.TerminalFailureReason.NON_RETRYABLE_FAILURE
                    );

            default -> throw new IllegalStateException(
                    "Unexpected processing job status after failure: "
                            + job.getStatus()
            );
        };
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
