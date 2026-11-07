
package com.lorenzosanz.uploadservice.processing.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "processing_job",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processing_job_upload",
                        columnNames = "upload_id"
                )
        }
)
public class ProcessingJob {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProcessingJobStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "output_path", length = 1024)
    private String outputPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ProcessingJob() {
    }

    public ProcessingJob(UUID jobId, UUID uploadId, Instant createdAt) {
        this.jobId = jobId;
        this.uploadId = uploadId;
        this.status = ProcessingJobStatus.PENDING;
        this.attempts = 0;
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getUploadId() {
        return uploadId;
    }

    public ProcessingJobStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public UUID getClaimToken() {
        return claimToken;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public String getLastError() {
        return lastError;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isClaimableAt(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time is required");
        }

        if (status == ProcessingJobStatus.PENDING) {
            return nextAttemptAt != null
                    && !nextAttemptAt.isAfter(now);
        }

        if (status == ProcessingJobStatus.PROCESSING) {
            return leaseUntil != null
                    && !leaseUntil.isAfter(now);
        }

        return false;
    }

    public boolean markFailedIfAttemptsExhausted(
            Instant now,
            int maxAttempts) {

        if (now == null) {
            throw new IllegalArgumentException(
                    "Current time is required"
            );
        }

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be at least one"
            );
        }

        if (attempts < maxAttempts || !isClaimableAt(now)) {
            return false;
        }

        this.status = ProcessingJobStatus.FAILED;
        this.lastError = "Maximum processing attempts reached";
        this.claimToken = null;
        this.leaseUntil = null;

        return true;
    }

    public void claim(
            UUID token,
            Instant claimedAt,
            Instant newLeaseUntil) {

        if (token == null || claimedAt == null || newLeaseUntil == null) {
            throw new IllegalArgumentException(
                    "Claim token and timestamps are required"
            );
        }

        if (!newLeaseUntil.isAfter(claimedAt)) {
            throw new IllegalArgumentException(
                    "Lease expiration must be after claim time"
            );
        }

        if (!isClaimableAt(claimedAt)) {
            throw new IllegalStateException(
                    "Processing job is not claimable"
            );
        }

        this.status = ProcessingJobStatus.PROCESSING;
        this.attempts++;
        this.claimToken = token;
        this.leaseUntil = newLeaseUntil;
        this.lastHeartbeatAt = claimedAt;

        if (this.startedAt == null) {
            this.startedAt = claimedAt;
        }
    }

    public boolean renewLease(
            UUID expectedClaimToken,
            Instant now,
            Instant requestedLeaseUntil) {

        if (expectedClaimToken == null
                || now == null
                || requestedLeaseUntil == null) {
            throw new IllegalArgumentException(
                    "Claim token and timestamps are required"
            );
        }

        if (!requestedLeaseUntil.isAfter(now)) {
            throw new IllegalArgumentException(
                    "New lease expiration must be in the future"
            );
        }

        if (status != ProcessingJobStatus.PROCESSING
                || claimToken == null
                || !claimToken.equals(expectedClaimToken)
                || leaseUntil == null
                || !leaseUntil.isAfter(now)) {
            return false;
        }

        if (requestedLeaseUntil.isAfter(leaseUntil)) {
            this.leaseUntil = requestedLeaseUntil;
        }

        this.lastHeartbeatAt = now;

        return true;
    }

    public boolean complete(
            UUID expectedClaimToken,
            Instant completedAt,
            String outputPath) {

        if (expectedClaimToken == null || completedAt == null) {
            throw new IllegalArgumentException(
                    "Claim token and completion time are required"
            );
        }

        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException(
                    "Output path is required"
            );
        }

        if (status != ProcessingJobStatus.PROCESSING
                || claimToken == null
                || !claimToken.equals(expectedClaimToken)
                || leaseUntil == null
                || !leaseUntil.isAfter(completedAt)) {
            return false;
        }

        this.status = ProcessingJobStatus.COMPLETED;
        this.outputPath = outputPath.trim();
        this.completedAt = completedAt;
        this.claimToken = null;
        this.leaseUntil = null;
        this.lastError = null;

        return true;
    }

    public boolean fail(
            UUID expectedClaimToken,
            Instant failedAt,
            String errorMessage,
            int maxAttempts,
            Instant retryAt,
            boolean retryable) {

        if (expectedClaimToken == null
                || failedAt == null
                || errorMessage == null
                || errorMessage.isBlank()
                || retryAt == null) {
            throw new IllegalArgumentException(
                    "Claim token, failure time, error and retry time are required"
            );
        }

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be at least one"
            );
        }

        if (retryable && !retryAt.isAfter(failedAt)) {
            throw new IllegalArgumentException(
                    "Retry time must be after the failure time"
            );
        }

        // An old claim cannot modify a new job.
        if (status != ProcessingJobStatus.PROCESSING
                || claimToken == null
                || !claimToken.equals(expectedClaimToken)
                || leaseUntil == null
                || !leaseUntil.isAfter(failedAt)) {
            return false;
        }

        String normalizedError = errorMessage.trim();

        this.lastError = normalizedError.length() > 2000
                ? normalizedError.substring(0, 2000)
                : normalizedError;

        this.claimToken = null;
        this.leaseUntil = null;

        if (retryable && attempts < maxAttempts) {
            this.status = ProcessingJobStatus.PENDING;
            this.nextAttemptAt = retryAt;
        } else {
            this.status = ProcessingJobStatus.FAILED;
        }

        return true;
    }
}
