package com.lorenzosanz.uploadservice.service;

import org.springframework.stereotype.Service;
import com.lorenzosanz.uploadservice.dto.HealthResponse;

@Service
public class HealthService {

  public HealthResponse getHealth() {
    return new HealthResponse("Up", "upload-service");
  }
}
