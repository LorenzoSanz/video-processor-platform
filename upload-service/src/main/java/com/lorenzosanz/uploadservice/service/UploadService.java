package com.lorenzosanz.uploadservice.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.dto.UploadResponse;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.repository.UploadRepository;
import com.lorenzosanz.uploadservice.storage.StorageService;
import com.lorenzosanz.uploadservice.validation.FileSignatureValidator;
import com.lorenzosanz.uploadservice.validation.FileValidator;
import com.lorenzosanz.uploadservice.enums.UploadStatus;

@Service
public class UploadService {

  private final UploadRepository uploadRepository;
  private final StorageService storageService;
  private final FileValidator fileValidator;
  private final FileSignatureValidator fileSignatureValidator;

  public UploadService(UploadRepository uploadRepository, StorageService storageService, FileValidator fileValidator, FileSignatureValidator fileSignatureValidator) {
    this.uploadRepository = uploadRepository;
    this.storageService = storageService;
    this.fileValidator = fileValidator;
    this.fileSignatureValidator = fileSignatureValidator;
  }

  public UploadResponse startUpload(MultipartFile file) {
    UUID uploadId = UUID.randomUUID();
    String fileName = file.getOriginalFilename();
    String contentType = file.getContentType();
    long size = file.getSize();
    LocalDate uploadDate = LocalDate.now();

    fileValidator.validate(file);
    fileSignatureValidator.validate(file);

     Upload upload = new Upload(
      uploadId, 
      fileName, 
      UploadStatus.PENDING, 
      contentType, 
      size, 
      uploadDate);
    
    storageService.storeFile(upload, file);

    Upload savedUpload = uploadRepository.save(upload);

    return new UploadResponse(UploadStatus.PENDING, savedUpload.getUploadId());
  }
}
