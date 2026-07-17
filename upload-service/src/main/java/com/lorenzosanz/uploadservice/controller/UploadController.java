package com.lorenzosanz.uploadservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lorenzosanz.uploadservice.dto.UploadRequest;
import com.lorenzosanz.uploadservice.service.UploadService;

import jakarta.validation.Valid;

import com.lorenzosanz.uploadservice.dto.UploadResponse;

@RestController
@RequestMapping("/upload")
public class UploadController {

  private final UploadService uploadService;

  public UploadController(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping
  public UploadResponse postUpload(@Valid @RequestBody UploadRequest uploadRequest) {
    return uploadService.startUpload(uploadRequest);
  }
}
