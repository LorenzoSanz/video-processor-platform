package com.lorenzosanz.uploadservice.processing.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.upload.domain.UploadStatus;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

@Service
public class ProcessingJobClaimService {

    private final ProcessingJobRepository processingJobRepository;

    private final UploadRepository uploadRepository;

    public ProcessingJobClaimService(
            ProcessingJobRepository processingJobRepository, UploadRepository uploadRepository) {
        this.processingJobRepository = processingJobRepository;
        this.uploadRepository = uploadRepository;
    }

    @Transactional
    public Optional<ProcessingJob> claim(
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
            return Optional.empty();
        }

        Instant now = Instant.now();
        ProcessingJob job = candidate.get();

        if (!job.isClaimableAt(now)) {
            return Optional.empty();
        }

        Upload upload = uploadRepository.findById(job.getUploadId())
        .orElseThrow(() -> new IllegalStateException(
                "Processing job references a missing upload"
        ));

        if (upload.getStatus() == UploadStatus.COMPLETED
        || upload.getStatus() == UploadStatus.FAILED) {
            throw new IllegalStateException(
                "Cannot claim a job whose upload is in a terminal state"
            );
        }

        UUID claimToken = UUID.randomUUID();
        Instant leaseUntil = now.plus(leaseDuration);

        job.claim(claimToken, now, leaseUntil);

        upload.setStatus(UploadStatus.PROCESSING);

        processingJobRepository.flush();

        return Optional.of(job);
    }
}