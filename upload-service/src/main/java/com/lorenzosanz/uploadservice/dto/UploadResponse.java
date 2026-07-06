package com.lorenzosanz.uploadservice.dto;

public class UploadResponse {

  private String status;
  private String uploadId;

  public UploadResponse() {
  }

  public UploadResponse(String status, String uploadId) {
    this.status = status;
    this.uploadId = uploadId;
  }

  public String getStatus() {
    return status;
  }

  public String getUploadId() {
    return uploadId;
  }
}
