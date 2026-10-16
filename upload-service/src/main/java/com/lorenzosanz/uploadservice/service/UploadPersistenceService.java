package com.lorenzosanz.uploadservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.repository.OutboxEventRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

@Service
public class UploadPersistenceService {

    private final UploadRepository uploadRepository;
    private final OutboxEventRepository outboxEventRepository;

    public UploadPersistenceService(
            UploadRepository uploadRepository,
            OutboxEventRepository outboxEventRepository) {
        this.uploadRepository = uploadRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public Upload persistUploadAndOutbox(Upload upload) {

        Upload savedUpload = uploadRepository.save(upload);

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