package com.lorenzosanz.uploadservice.upload.api;

import java.util.UUID;

import com.lorenzosanz.uploadservice.upload.domain.UploadStatus;

public class UploadResponse {

  private UploadStatus status;
  private UUID uploadId;

  public UploadResponse() {
  }

  public UploadResponse(UploadStatus status, UUID uploadId) {
    this.status = status;
    this.uploadId = uploadId;
  }

  public UploadStatus getStatus() {
    return status;
  }

  public UUID getUploadId() {
    return uploadId;
  }
}
