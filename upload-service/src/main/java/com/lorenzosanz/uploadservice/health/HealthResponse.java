package com.lorenzosanz.uploadservice.health;

public class HealthResponse {
  private String status;
  private String service;

  public HealthResponse() {
  }

  public HealthResponse(String status, String service) {
    this.status = status;
    this.service = service;
  }

  public String getStatus() {
    return status;
  }

  public String getService() {
    return service;
  }

}
