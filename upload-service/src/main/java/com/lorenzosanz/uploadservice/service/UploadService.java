package com.lorenzosanz.uploadservice.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lorenzosanz.uploadservice.dto.UploadRequest;
import com.lorenzosanz.uploadservice.dto.UploadResponse;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.repository.UploadRepository;
import com.lorenzosanz.uploadservice.enums.UploadStatus;

@Service
public class UploadService {

  private final UploadRepository uploadRepository;

  public UploadService(UploadRepository uploadRepository) {
    this.uploadRepository = uploadRepository;
  }

  public UploadResponse startUpload(UploadRequest uploadRequest) {

    Upload upload = new Upload(UUID.randomUUID(), uploadRequest.getFileName(), UploadStatus.PENDING,
        uploadRequest.getContentType(),
        0, LocalDate.now());

    Upload savedUpload = uploadRepository.save(upload);

    return new UploadResponse(UploadStatus.PROCESSING, upload.getUploadId());
  }
}
