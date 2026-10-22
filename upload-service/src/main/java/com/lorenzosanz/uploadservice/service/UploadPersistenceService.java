package com.lorenzosanz.uploadservice.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.entity.ProcessingJob;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.repository.OutboxEventRepository;
import com.lorenzosanz.uploadservice.repository.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

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