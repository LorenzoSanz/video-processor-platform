package com.lorenzosanz.uploadservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UploadRequest {

  @NotBlank(message = "File name is required.")
  @Size(max = 255)
  private String fileName;

  @NotBlank(message = "Content Type is required.")
  @Size(max = 255)
  private String contentType;

  public UploadRequest() {
  }

  public UploadRequest(String name, String contentType) {
    this.fileName = name;
    this.contentType = contentType;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public void setFileName(String name) {
    this.fileName = name;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}
