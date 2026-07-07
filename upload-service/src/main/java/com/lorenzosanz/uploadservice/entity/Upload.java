package com.lorenzosanz.uploadservice.entity;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import com.lorenzosanz.uploadservice.enums.UploadStatus;

@Entity
public class Upload {

  @Id
  private UUID uploadId;

  private String fileName;
  private UploadStatus status;
  private String contentType;
  private long size;
  private LocalDate date;
  private String user;

  public Upload() {
  }

  public Upload(UUID uploadId, String fileName, UploadStatus status, String contentType, long size, String user,
      LocalDate date) {
    this.uploadId = uploadId;
    this.fileName = fileName;
    this.status = status;
    this.contentType = contentType;
    this.size = size;
    this.user = user;
    this.date = date;
  }

  public UUID getUploadId() {
    return uploadId;
  }

  public String getFileName() {
    return fileName;
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

  public String getUser() {
    return user;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setUploadId(UUID uploadId) {
    this.uploadId = uploadId;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setStatus(UploadStatus status) {
    this.status = status;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }
}
