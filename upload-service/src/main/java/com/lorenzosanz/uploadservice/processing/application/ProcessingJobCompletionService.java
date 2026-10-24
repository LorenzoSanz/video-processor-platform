package com.lorenzosanz.uploadservice.processing.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;

@Service
public class ProcessingJobCompletionService {

    private final ProcessingJobRepository processingJobRepository;

    public ProcessingJobCompletionService(
            ProcessingJobRepository processingJobRepository) {
        this.processingJobRepository = processingJobRepository;
    }

    @Transactional
    public boolean complete(
            UUID jobId,
            UUID claimToken,
            String outputPath) {

        if (jobId == null || claimToken == null) {
            throw new IllegalArgumentException(
                    "Job ID and claim token are required"
            );
        }

        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException(
                    "Output path is required"
            );
        }

        Instant now = Instant.now();

        return processingJobRepository.findByJobIdForUpdate(jobId)
                .map(job -> job.complete(
                        claimToken,
                        now,
                        outputPath
                ))
                .orElse(false);
    }
}