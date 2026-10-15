package com.lorenzosanz.uploadservice.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@DataJpaTest
class UploadRepositoryTest {
  
  @Autowired
  private UploadRepository uploadRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  void shouldHaveUpload(){
    Upload upload = new Upload(UUID.randomUUID(), "test.mp4", UploadStatus.PENDING ,"video/mp4", 1000L, Instant.now());

    uploadRepository.saveAndFlush(upload);

    entityManager.clear();

    Optional<Upload> result = uploadRepository.findById(upload.getUploadId());

    assertTrue(result.isPresent());
  }

  @Test 
  void shouldMaintainDataIntegrity() {
    Upload upload = new Upload(UUID.randomUUID(), "test.mp4", UploadStatus.PENDING , "video/mp4", 10000L, Instant.now());

    uploadRepository.saveAndFlush(upload);

    entityManager.clear();

    Optional<Upload> result = uploadRepository.findById(upload.getUploadId());

    assertTrue(result.isPresent());

    Upload savedUpload = result.get();
   
    assertEquals(upload.getFileName(), savedUpload.getFileName());
    assertEquals(upload.getUploadId(), savedUpload.getUploadId());
    assertEquals(upload.getStatus(), savedUpload.getStatus());
    assertEquals(upload.getContentType(), savedUpload.getContentType());
    assertEquals(upload.getSize(), savedUpload.getSize());
    assertTrue(
    Duration.between(upload.getCreatedAt(), savedUpload.getCreatedAt())
        .abs()
        .compareTo(Duration.ofNanos(1_000)) <= 0
);
  }

  @Test 
  void shouldReturnEmptyWhenUploadDoesNotExist() {
    Optional<Upload> emptyResult = uploadRepository.findById(UUID.randomUUID());
    assertTrue(emptyResult.isEmpty());
  }
}
