package com.lorenzosanz.uploadservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.repository.ProcessingJobRepository;

@Service
public class ProcessingJobHeartbeatService {

    private final ProcessingJobRepository processingJobRepository;

    public ProcessingJobHeartbeatService(
            ProcessingJobRepository processingJobRepository) {
        this.processingJobRepository = processingJobRepository;
    }

    @Transactional
    public boolean heartbeat(
            UUID jobId,
            UUID claimToken,
            Duration leaseDuration) {

        if (jobId == null || claimToken == null) {
            throw new IllegalArgumentException(
                    "Job ID and claim token are required"
            );
        }

        if (leaseDuration == null
                || leaseDuration.isZero()
                || leaseDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "Lease duration must be positive"
            );
        }

        Instant now = Instant.now();

        return processingJobRepository.findByJobIdForUpdate(jobId)
                .map(job -> job.renewLease(
                        claimToken,
                        now,
                        now.plus(leaseDuration)
                ))
                .orElse(false);
    }
}