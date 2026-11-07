package com.lorenzosanz.uploadservice.processing.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;

@Service
public class ProcessingJobClaimService {

    private final ProcessingJobRepository processingJobRepository;

    public ProcessingJobClaimService(
            ProcessingJobRepository processingJobRepository) {
        this.processingJobRepository = processingJobRepository;
    }

    @Transactional
    public ProcessingJobClaimResult claim(
            UUID uploadId,
            Duration leaseDuration) {

        if (uploadId == null) {
            throw new IllegalArgumentException("Upload ID is required");
        }

        if (leaseDuration == null
                || leaseDuration.isZero()
                || leaseDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "Lease duration must be positive"
            );
        }

        Optional<ProcessingJob> candidate =
                processingJobRepository.findByUploadIdForUpdate(uploadId);

        if (candidate.isEmpty()) {
            return new ProcessingJobClaimResult.NotFound();
        }

        Instant now = Instant.now();
        ProcessingJob job = candidate.get();

        if (!job.isClaimableAt(now)) {
            return switch (job.getStatus()) {
                case PENDING ->
                        new ProcessingJobClaimResult.RetryNotDue(
                                job.getNextAttemptAt()
                        );
        
                case PROCESSING ->
                        new ProcessingJobClaimResult.LeaseActive(
                                job.getLeaseUntil()
                        );
        
                case COMPLETED ->
                        new ProcessingJobClaimResult.AlreadyCompleted();
        
                case FAILED ->
                        new ProcessingJobClaimResult.TerminallyFailed();
            };
        }

        UUID claimToken = UUID.randomUUID();
        Instant leaseUntil = now.plus(leaseDuration);

        job.claim(claimToken, now, leaseUntil);

        processingJobRepository.flush();

        return new ProcessingJobClaimResult.Claimed(job);
    }
}