package com.lorenzosanz.uploadservice.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import com.lorenzosanz.uploadservice.dto.upload.UploadResponse;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.exception.FileValidationException;
import com.lorenzosanz.uploadservice.exception.StorageException;
import com.lorenzosanz.uploadservice.repository.UploadRepository;
import com.lorenzosanz.uploadservice.storage.StorageService;
import com.lorenzosanz.uploadservice.validation.FileSignatureValidator;
import com.lorenzosanz.uploadservice.validation.FileValidator;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {
  @Mock
  private MultipartFile file;
  
  @Mock
  private UploadRepository uploadRepository;
  
  @Mock
  private StorageService storageService;

  @Mock
  private FileValidator fileValidator;

  @Mock
  private FileSignatureValidator fileSignatureValidator;

  @InjectMocks
  private UploadService uploadService;

  @BeforeEach
  void setUp() {
      when(file.getOriginalFilename()).thenReturn("image.gif");
      when(file.getContentType()).thenReturn("image/gif");
      when(file.getSize()).thenReturn(20000L);
  }

  @Test
  void shouldStartUploadSuccessfully() {
    Upload savedUpload = new Upload(
    UUID.randomUUID(), 
    file.getOriginalFilename(),
    UploadStatus.PENDING, 
    file.getContentType(), 
    file.getSize(),
    Instant.now()
    );

    when(uploadRepository.save(any(Upload.class))).thenReturn(savedUpload);

    UploadResponse response = uploadService.startUpload(file);

    assertEquals(UploadStatus.PENDING, response.getStatus());

    assertEquals(savedUpload.getUploadId(), response.getUploadId());

    verify(fileValidator).validate(file);
    verify(fileSignatureValidator).validate(file);

    verify(storageService).storeFile(
      any(Upload.class),
      any(MultipartFile.class)
  );
    verify(uploadRepository).save(any(Upload.class));
  }

  @Test
  void shouldDeleteStoredFileWhenRepositoryFails() {
    when(uploadRepository.save(any(Upload.class))).thenThrow(new RuntimeException("Database error"));

    assertThrows(RuntimeException.class,() -> uploadService.startUpload(file));

    verify(uploadRepository).save(any(Upload.class));
    verify(storageService).deleteFile(any());
  }

  @Test
  void shouldNotSaveUploadWhenStorageFails() {
    StorageException ex = new StorageException("Storage Error");

    doThrow(ex)
    .when(storageService)
    .storeFile(any(Upload.class), any(MultipartFile.class));

    assertThrows(
      StorageException.class,
      () -> uploadService.startUpload(file)
    );
    
    verify(uploadRepository, never()).save(any(Upload.class));
  }

  @Test
  void shouldStopProcessWhenValidationFails() {
    FileValidationException ex = new FileValidationException("File Validation Exception");

    doThrow(ex)
    .when(fileValidator)
    .validate(any(MultipartFile.class));

    assertThrows(
      FileValidationException.class,
      () -> uploadService.startUpload(file)
    );

    verify(fileSignatureValidator, never()).validate(file);

    verify(storageService, never()).storeFile(any(Upload.class), any(MultipartFile.class));

    verify(uploadRepository, never()).save(any(Upload.class));
  }

  @Test
  void shouldStopProcessWhenSignatureValidationFails() {
    FileValidationException ex = new FileValidationException("File signature Validation exception");

    doThrow(ex)
    .when(fileSignatureValidator)
    .validate(any(MultipartFile.class));

    assertThrows(
      FileValidationException.class,
      () -> uploadService.startUpload(file));

    verify(fileValidator).validate(file);

    verify(storageService, never()).storeFile(any(Upload.class), any(MultipartFile.class));

    verify(uploadRepository, never()).save(any(Upload.class));
  }
}
