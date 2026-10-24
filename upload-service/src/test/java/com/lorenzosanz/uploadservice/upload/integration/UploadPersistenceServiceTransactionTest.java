package com.lorenzosanz.uploadservice.upload.integration;

import com.lorenzosanz.uploadservice.upload.application.UploadPersistenceService;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.outbox.domain.OutboxEvent;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.upload.domain.UploadStatus;
import com.lorenzosanz.uploadservice.outbox.infrastructure.persistence.OutboxEventRepository;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class UploadPersistenceServiceTransactionTest {

    @Autowired
    private UploadPersistenceService uploadPersistenceService;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldRollbackUploadWhenOutboxPersistenceFails() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "test-video.mp4",
                UploadStatus.PENDING,
                "video/mp4",
                1024L,
                Instant.now()
        );

        doAnswer(invocation -> {
            uploadRepository.flush();

            assertTrue(uploadRepository.existsById(uploadId));

            throw new IllegalStateException("Simulated Outbox failure");

        }).when(outboxEventRepository).save(any(OutboxEvent.class));

        assertThrows(
                IllegalStateException.class,
                () -> uploadPersistenceService.persistUploadAndOutbox(upload)
        );

        assertFalse(uploadRepository.existsById(uploadId));

        assertTrue(processingJobRepository.findByUploadId(uploadId).isEmpty());
    }
}