package com.lorenzosanz.uploadservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lorenzosanz.uploadservice.entity.Upload;

public interface UploadRepository extends JpaRepository<Upload, UUID> {

}
