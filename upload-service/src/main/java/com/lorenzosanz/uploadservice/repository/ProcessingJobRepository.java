package com.lorenzosanz.uploadservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lorenzosanz.uploadservice.entity.ProcessingJob;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, UUID> {

    Optional<ProcessingJob> findByUploadId(UUID uploadId);
}