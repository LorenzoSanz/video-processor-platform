package com.lorenzosanz.uploadservice.upload.api;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.upload.application.UploadService;

@RestController
@RequestMapping("/upload")
public class UploadController {

  private final UploadService uploadService;

  public UploadController(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping
  public UploadResponse postUpload(@RequestParam("file") MultipartFile file) {

    return uploadService.startUpload(file);
  }
}
