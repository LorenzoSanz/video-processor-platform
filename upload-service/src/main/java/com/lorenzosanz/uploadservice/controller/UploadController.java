package com.lorenzosanz.uploadservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lorenzosanz.uploadservice.dto.UploadRequest;

@RestController
@RequestMapping("/upload")
public class UploadController {

  private final UploadService uploadService;

  public UploadController(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping
  public UploadRequest getUpload(UploadRequest UploadRequest) {
    return uploadService.getUpload();
  }
}
