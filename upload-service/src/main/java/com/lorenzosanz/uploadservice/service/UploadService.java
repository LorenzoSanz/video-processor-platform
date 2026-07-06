package com.lorenzosanz.uploadservice.service;

import org.springframework.stereotype.Service;

import com.lorenzosanz.uploadservice.dto.UploadRequest;
import com.lorenzosanz.uploadservice.dto.UploadResponse;

@Service
public class UploadService {

  public UploadResponse startUpload(UploadRequest uploadRequest) {
    return new UploadResponse("Success", "45gkdfgkodasw");
  }
}
