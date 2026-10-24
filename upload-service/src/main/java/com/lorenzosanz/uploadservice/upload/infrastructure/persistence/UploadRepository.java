package com.lorenzosanz.uploadservice.upload.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lorenzosanz.uploadservice.upload.domain.Upload;

public interface UploadRepository extends JpaRepository<Upload, UUID> {

}
