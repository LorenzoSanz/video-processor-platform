package com.lorenzosanz.uploadservice.dto.error;

public class ValidationError {

  private String field;
  private String message;

  public ValidationError() {}

  public ValidationError(String field, String message) {
    this.field = field;
    this.message = message;
  }

  public String getField() {
    return field;
  }

  public String getMessage() {
    return message;
  }

  public void setField(String field) {
    this.field = field;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
