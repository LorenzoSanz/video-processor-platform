package com.lorenzosanz.uploadservice.dto;

public class UploadRequest {

  private String fileName;
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
