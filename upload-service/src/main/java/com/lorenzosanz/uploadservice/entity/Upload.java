package com.lorenzosanz.uploadservice.entity;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import com.lorenzosanz.uploadservice.enums.UploadStatus;

@Entity
public class Upload {

  @Id
  private UUID uploadId;
  private UploadStatus status;
  private String contentType;
  private float size;

  @Foreign Key
  private String user;
  private LocalDate date;

  public Upload() {
  }

  public Upload(UUID uploadId, UploadStatus status, String contentType, float size, String user, LocalDate date) {
    this.uploadId = uploadId;
    this.status = status;
    this.contentType = contentType;
    this.size = size;
    this.user = user;
    this.date = date;
  }

  public UUID getUploadId() {
    return uploadId;
  }

  public UploadStatus getStatus() {
    return status;
  }

  public String getContentType() {
    return contentType;
  }

  public float getSize() {
    return size;
  }

  public String getUser() {
    return user;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setUploadId(UUID uploadId) {
    this.uploadId = uploadId;
  }

  public void setStatus(UploadStatus status) {
    this.status = status;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setSize(float size) {
    this.size = size;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }
}
