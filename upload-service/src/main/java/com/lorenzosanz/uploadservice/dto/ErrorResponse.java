package com.lorenzosanz.uploadservice.dto;

import java.time.LocalDateTime;

public class ErrorResponse {
  private int status;
  private String message;
  private LocalDateTime time;

  public ErrorResponse() {
  }

  public ErrorResponse(int errorStatus, String errorMessage, LocalDateTime errorTime) {
    this.status = errorStatus;
    this.message = errorMessage;
    this.time = errorTime;
  }

  public int getErrorStatus() {
    return status;
  }

  public String getErrorMessage() {
    return message;
  }

  public LocalDateTime getErrorTime() {
    return time;
  }

  public void setErrorStatus(int errorStatus) {
    this.status = errorStatus;
  }

  public void setErrorMessage(String errorMessage) {
    this.message = errorMessage;
  }

  public void setErrorTime(LocalDateTime errorTime) {
    this.time = errorTime;
  }
}