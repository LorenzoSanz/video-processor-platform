package com.lorenzosanz.uploadservice.upload.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
public class Upload {

    @Id
    @NotNull
    @Column(nullable = false)
    private UUID uploadId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String fileName;

    private String storagePath;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private UploadStatus status;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String contentType;

    @Positive
    @Column(nullable = false)
    private long size;

    @NotNull
    @Column(nullable = false)
    private Instant createdAt;

    protected Upload() {
        // Requerido por JPA.
    }

    public Upload(
            UUID uploadId,
            String fileName,
            UploadStatus status,
            String contentType,
            long size,
            Instant createdAt) {

        this.uploadId = uploadId;
        this.fileName = fileName;
        this.status = status;
        this.contentType = contentType;
        this.size = size;
        this.createdAt = createdAt;
    }

    public UUID getUploadId() {
        return uploadId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void assignStoragePath(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Storage path is required"
            );
        }

        if (this.storagePath != null) {
            throw new IllegalStateException(
                    "Storage path has already been assigned"
            );
        }

        this.storagePath = storagePath;
    }

    public void markProcessing() {
        // PROCESSING también se permite porque otro worker puede
        // recuperar un trabajo cuyo lease haya vencido.
        if (status != UploadStatus.PENDING
                && status != UploadStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Cannot process an upload in status " + status
            );
        }

        this.status = UploadStatus.PROCESSING;
    }

    public void markCompleted() {
        requireProcessing("complete");
        this.status = UploadStatus.COMPLETED;
    }

    public void markPendingForRetry() {
        requireProcessing("schedule a retry for");
        this.status = UploadStatus.PENDING;
    }

    public void markFailed() {
        requireProcessing("fail");
        this.status = UploadStatus.FAILED;
    }

    private void requireProcessing(String operation) {
        if (status != UploadStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Cannot " + operation
                            + " an upload in status " + status
            );
        }
    }
}