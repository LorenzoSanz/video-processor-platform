package com.lorenzosanz.uploadservice.upload.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.outbox.domain.OutboxEvent;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.outbox.infrastructure.persistence.OutboxEventRepository;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

@Service
public class UploadPersistenceService {

    private final UploadRepository uploadRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessingJobRepository processingJobRepository;

    public UploadPersistenceService(
            UploadRepository uploadRepository,
            OutboxEventRepository outboxEventRepository,
            ProcessingJobRepository processingJobRepository) {
        this.uploadRepository = uploadRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.processingJobRepository = processingJobRepository;
    }

    @Transactional
    public Upload persistUploadAndOutbox(Upload upload) {

        Upload savedUpload = uploadRepository.save(upload);

        ProcessingJob processingJob = new ProcessingJob(
                UUID.randomUUID(),
                savedUpload.getUploadId(),
                savedUpload.getCreatedAt()
        );

        processingJobRepository.save(processingJob);

        String payload = "{\"uploadId\":\""
                + savedUpload.getUploadId()
                + "\"}";

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                savedUpload.getUploadId(),
                "VIDEO_PROCESSING_REQUESTED",
                payload
        );

        outboxEventRepository.save(event);

        return savedUpload;
    }
}