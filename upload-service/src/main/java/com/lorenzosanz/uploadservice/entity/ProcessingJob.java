package com.lorenzosanz.uploadservice.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.lorenzosanz.uploadservice.enums.ProcessingJobStatus;

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
}