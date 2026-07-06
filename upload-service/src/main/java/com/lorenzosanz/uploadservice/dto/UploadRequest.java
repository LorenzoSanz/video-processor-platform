package com.lorenzosanz.uploadservice.dto;

public class UploadRequest {

  private String name;
  private String contentType;

  public UploadRequest() {
  }

  public UploadRequest(String name, String contentType) {
    this.name = name;
    this.contentType = contentType;
  }

  public String getName() {
    return name;
  }

  public String getContentType() {
    return contentType;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}
