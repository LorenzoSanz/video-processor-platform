package com.lorenzosanz.uploadservice.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;

@DataJpaTest
class UploadRepositoryTest {
  
  @Autowired
  private UploadRepository uploadRepository;

  @Test
  void shouldHaveUpload(){
    Upload upload = new Upload(UUID.randomUUID(), "test", UploadStatus.PENDING , null, 0, LocalDateTime.now());

    uploadRepository.save(upload);
    Optional<Upload> result = uploadRepository.findById(upload.getUploadId());

    assertTrue(result.isPresent());
  }

  @Test 
  void shouldMaintainDataIntegrity() {
    Upload upload = new Upload(UUID.randomUUID(), "test", UploadStatus.PENDING , "mp4", 10000L, LocalDateTime.now());

    uploadRepository.save(upload);
    Optional<Upload> result = uploadRepository.findById(upload.getUploadId());
    Upload savedUpload = result.get();
   
    assertEquals(upload.getFileName(), savedUpload.getFileName());
    assertEquals(upload.getUploadId(), savedUpload.getUploadId());
    assertEquals(upload.getStatus(), savedUpload.getStatus());
    assertEquals(upload.getContentType(), savedUpload.getContentType());
    assertEquals(upload.getSize(), savedUpload.getSize());
    assertEquals(upload.getCreatedAt(), savedUpload.getCreatedAt());
  }

  @Test 
  void shouldReturnEmptyWhenUploadDoesNotExist() {
    Optional<Upload> emptyResult = uploadRepository.findById(UUID.randomUUID());
    assertTrue(emptyResult.isEmpty());
  }
}
