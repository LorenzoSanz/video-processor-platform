package com.lorenzosanz.uploadservice.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.dto.UploadRequest;
import com.lorenzosanz.uploadservice.dto.UploadResponse;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.repository.UploadRepository;
import com.lorenzosanz.uploadservice.storage.StorageService;
import com.lorenzosanz.uploadservice.enums.UploadStatus;

@Service
public class UploadService {

  private final UploadRepository uploadRepository;
  private final StorageService storageService;

  public UploadService(UploadRepository uploadRepository, StorageService StorageService) {
    this.uploadRepository = uploadRepository;
    this.storageService = StorageService;
  }

  public UploadResponse startUpload(MultipartFile file, UploadRequest uploadRequest) {
    String fileName = file.getOriginalFilename();
    String contentType = file.getContentType();
    long size = file.getSize();

    Upload upload = new Upload(UUID.randomUUID(), fileName, UploadStatus.PENDING, contentType, size, LocalDate.now());

    Upload savedUpload = uploadRepository.save(upload);

    return new UploadResponse(UploadStatus.PENDING, savedUpload.getUploadId());
  }
}
