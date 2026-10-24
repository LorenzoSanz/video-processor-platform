package com.lorenzosanz.uploadservice.upload.integration;

import com.lorenzosanz.uploadservice.upload.application.UploadPersistenceService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.outbox.domain.OutboxEvent;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.upload.domain.UploadStatus;
import com.lorenzosanz.uploadservice.outbox.infrastructure.persistence.OutboxEventRepository;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class UploadPersistenceServiceIntegrationTest {

    @Autowired
    private UploadPersistenceService uploadPersistenceService;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

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

            ProcessingJob job = processingJobRepository.findByUploadId(uploadId)
        .orElseThrow();

        assertNotNull(job.getJobId());
        assertEquals(uploadId, job.getUploadId());
        assertEquals(ProcessingJobStatus.PENDING, job.getStatus());
        assertEquals(0, job.getAttempts());
        assertNotNull(job.getCreatedAt());
        assertNotNull(job.getNextAttemptAt());
        assertNull(job.getClaimToken());
        assertNull(job.getLeaseUntil());
        assertNull(job.getLastHeartbeatAt());
        assertNull(job.getOutputPath());

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
            
            processingJobRepository.findByUploadId(uploadId)
                    .ifPresent(job -> processingJobRepository.deleteById(job.getJobId()));        

            if (uploadRepository.existsById(uploadId)) {
                uploadRepository.deleteById(uploadId);
            }
        }
    }
}