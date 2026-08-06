package com.lorenzosanz.uploadservice.dto.upload;

import java.util.UUID;

import com.lorenzosanz.uploadservice.enums.UploadStatus;

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
