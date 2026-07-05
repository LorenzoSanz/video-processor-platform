package com.lorenzosanz.uploadservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lorenzosanz.uploadservice.dto.HealthResponse;
import com.lorenzosanz.uploadservice.service.HealthService;

@RestController
@RequestMapping("/health")
public class HealthController {

  private final HealthService healthService;

  public HealthController(HealthService healthService) {
    this.healthService = healthService;
  }

  @GetMapping
  public HealthResponse getHealth() {
    return healthService.getHealth();
  }

}
