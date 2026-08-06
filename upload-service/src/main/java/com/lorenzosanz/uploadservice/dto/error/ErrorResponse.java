package com.lorenzosanz.uploadservice.dto.error;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {
  private int status;
  private String message;
  private LocalDateTime timestamp;
  private String path;
  private List<ValidationError> errors;

  public ErrorResponse() {
  }

  public ErrorResponse(int status, String message, LocalDateTime timestamp, String path) {
    this.status = status;
    this.message = message;
    this.timestamp = timestamp;
    this.path = path;
  }

  public ErrorResponse(int status, String message, LocalDateTime timestamp, String path, List<ValidationError> errors) {
    this.status = status;
    this.message = message;
    this.timestamp = timestamp;
    this.path = path;
    this.errors = errors;
  }

  public int getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  public List<ValidationError> getErrors() {
    return errors;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setErrors(List<ValidationError> errors) {
    this.errors = errors;
  }
}