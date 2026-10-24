package com.lorenzosanz.uploadservice.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.repository.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

@Service
public class ProcessingJobCompletionService {

    private final ProcessingJobRepository processingJobRepository;
    private final UploadRepository uploadRepository;

    public ProcessingJobCompletionService(
            ProcessingJobRepository processingJobRepository,
            UploadRepository uploadRepository) {
        this.processingJobRepository = processingJobRepository;
        this.uploadRepository = uploadRepository;
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
                .map(job -> {
                    boolean completed = job.complete(
                            claimToken,
                            now,
                            outputPath
                    );

                    if (!completed) {
                        return false;
                    }

                    Upload upload = uploadRepository.findById(job.getUploadId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Processing job references a missing upload"
                            ));

                    upload.setStatus(UploadStatus.COMPLETED);

                    return true;
                })
                .orElse(false);
    }
}