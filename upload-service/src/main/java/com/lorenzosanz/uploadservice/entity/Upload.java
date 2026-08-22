package com.lorenzosanz.uploadservice.entity;

import java.time.LocalDateTime;
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

import com.lorenzosanz.uploadservice.enums.UploadStatus;

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
  private LocalDateTime createdAt;

  public Upload() {
  }

  public Upload(UUID uploadId, String fileName, UploadStatus status, String contentType, long size,
    LocalDateTime createdAt) {
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
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

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public void setStoragePath(String storagePath) {
    this.storagePath = storagePath;
  }
}
