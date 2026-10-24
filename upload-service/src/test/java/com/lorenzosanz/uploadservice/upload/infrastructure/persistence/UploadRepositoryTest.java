package com.lorenzosanz.uploadservice.upload.infrastructure.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import org.springframework.context.annotation.Import;

import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.upload.domain.Upload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@DataJpaTest
@Import(PostgresTestContainerConfiguration.class)
class UploadRepositoryTest {

    @Autowired
    private UploadRepository uploadRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldHaveUpload() {
        Upload upload = new Upload(
                UUID.randomUUID(),
                "test.mp4",
                "video/mp4",
                1000L,
                Instant.now()
        );

        uploadRepository.saveAndFlush(upload);

        entityManager.clear();

        Optional<Upload> result =
                uploadRepository.findById(upload.getUploadId());

        assertTrue(result.isPresent());
    }

    @Test
    void shouldMaintainDataIntegrity() {
        Upload upload = new Upload(
                UUID.randomUUID(),
                "test.mp4",
                "video/mp4",
                10000L,
                Instant.now()
        );

        uploadRepository.saveAndFlush(upload);

        entityManager.clear();

        Optional<Upload> result =
                uploadRepository.findById(upload.getUploadId());

        assertTrue(result.isPresent());

        Upload savedUpload = result.get();

        assertEquals(upload.getFileName(), savedUpload.getFileName());
        assertEquals(upload.getUploadId(), savedUpload.getUploadId());
        assertEquals(upload.getContentType(), savedUpload.getContentType());
        assertEquals(upload.getSize(), savedUpload.getSize());
        assertEquals(upload.getStoragePath(), savedUpload.getStoragePath());

        assertTrue(
                Duration.between(
                        upload.getCreatedAt(),
                        savedUpload.getCreatedAt()
                )
                .abs()
                .compareTo(Duration.ofNanos(1_000)) <= 0
        );
    }

    @Test
    void shouldReturnEmptyWhenUploadDoesNotExist() {
        Optional<Upload> emptyResult =
                uploadRepository.findById(UUID.randomUUID());

        assertTrue(emptyResult.isEmpty());
    }
}