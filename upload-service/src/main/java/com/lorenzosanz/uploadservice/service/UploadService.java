package com.lorenzosanz.uploadservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.dto.upload.UploadResponse;
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

  private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

  public UploadService(UploadRepository uploadRepository, StorageService storageService, FileValidator fileValidator, FileSignatureValidator fileSignatureValidator) {
    this.uploadRepository = uploadRepository;
    this.storageService = storageService;
    this.fileValidator = fileValidator;
    this.fileSignatureValidator = fileSignatureValidator;
  }

  @Transactional
  public UploadResponse startUpload(MultipartFile file) {
    UUID uploadId = UUID.randomUUID();
    String fileName = file.getOriginalFilename();
    String contentType = file.getContentType();
    long size = file.getSize();
    LocalDateTime uploadDate = LocalDateTime.now();

    logger.info("Upload started: filename={}", fileName);

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

    try {
      Upload savedUpload = uploadRepository.save(upload);

      logger.info("Upload completed: filename={}", fileName);

      return new UploadResponse(UploadStatus.PENDING, savedUpload.getUploadId());
      
    } catch (RuntimeException e) {
        storageService.deleteFile(upload.getStoragePath());
        throw e;
      }
  }
}
