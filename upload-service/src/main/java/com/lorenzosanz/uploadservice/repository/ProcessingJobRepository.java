package com.lorenzosanz.uploadservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lorenzosanz.uploadservice.entity.ProcessingJob;

import jakarta.persistence.LockModeType;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, UUID> {

    Optional<ProcessingJob> findByUploadId(UUID uploadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT job FROM ProcessingJob job WHERE job.uploadId = :uploadId")
    Optional<ProcessingJob> findByUploadIdForUpdate(
        @Param("uploadId") UUID uploadId
    );
}