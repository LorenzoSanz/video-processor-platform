package com.lorenzosanz.uploadservice.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.repository.OutboxEventRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UploadPersistenceServiceIntegrationTest {

    @Autowired
    private UploadPersistenceService uploadPersistenceService;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldPersistUploadAndOutboxEventSuccessfully() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "test-video.mp4",
                UploadStatus.PENDING,
                "video/mp4",
                1024L,
                Instant.now()
        );

        try {
            Upload savedUpload =
                    uploadPersistenceService.persistUploadAndOutbox(upload);

            assertEquals(uploadId, savedUpload.getUploadId());

            assertTrue(uploadRepository.findById(uploadId).isPresent());

            Upload persistedUpload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(
                    UploadStatus.PENDING,
                    persistedUpload.getStatus()
            );

            List<OutboxEvent> events = outboxEventRepository.findAll()
                    .stream()
                    .filter(event -> uploadId.equals(event.getAggregateId()))
                    .toList();

            assertEquals(1, events.size());

            OutboxEvent event = events.get(0);

            assertNotNull(event.getEventId());
            assertEquals("UPLOAD", event.getAggregateType());
            assertEquals(uploadId, event.getAggregateId());
            assertEquals(
                    "VIDEO_PROCESSING_REQUESTED",
                    event.getEventType()
            );
            assertTrue(event.getPayload().contains(uploadId.toString()));
            assertNotNull(event.getCreatedAt());
            assertNull(event.getPublishedAt());
            assertEquals(0, event.getAttempts());

        } finally {
            outboxEventRepository.findAll()
                    .stream()
                    .filter(event -> uploadId.equals(event.getAggregateId()))
                    .map(OutboxEvent::getEventId)
                    .toList()
                    .forEach(outboxEventRepository::deleteById);

            if (uploadRepository.existsById(uploadId)) {
                uploadRepository.deleteById(uploadId);
            }
        }
    }
}