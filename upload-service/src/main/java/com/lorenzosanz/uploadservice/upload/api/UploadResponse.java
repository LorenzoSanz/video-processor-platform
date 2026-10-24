package com.lorenzosanz.uploadservice.upload.api;

import java.util.UUID;

public class UploadResponse {

    private String status;
    private UUID uploadId;

    public UploadResponse() {
    }

    public UploadResponse(String status, UUID uploadId) {
        this.status = status;
        this.uploadId = uploadId;
    }

    public String getStatus() {
        return status;
    }

    public UUID getUploadId() {
        return uploadId;
    }
}