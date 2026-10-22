package com.lorenzosanz.uploadservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.dto.upload.UploadResponse;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.exception.StorageException;
import com.lorenzosanz.uploadservice.storage.StorageService;
import com.lorenzosanz.uploadservice.validation.FileSignatureValidator;
import com.lorenzosanz.uploadservice.validation.FileValidator;

@Service
public class UploadService {

    private static final Logger logger =
            LoggerFactory.getLogger(UploadService.class);

    private final UploadPersistenceService uploadPersistenceService;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final FileSignatureValidator fileSignatureValidator;

    public UploadService(
            UploadPersistenceService uploadPersistenceService,
            StorageService storageService,
            FileValidator fileValidator,
            FileSignatureValidator fileSignatureValidator) {
        this.uploadPersistenceService = uploadPersistenceService;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
        this.fileSignatureValidator = fileSignatureValidator;
    }

    public UploadResponse startUpload(MultipartFile file) {
        UUID uploadId = UUID.randomUUID();
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();
        Instant uploadDate = Instant.now();

        logger.info("Upload started: filename={}", fileName);

        fileValidator.validate(file);
        fileSignatureValidator.validate(file);

        Upload upload = new Upload(
                uploadId,
                fileName,
                UploadStatus.PENDING,
                contentType,
                size,
                uploadDate
        );

        try (InputStream inputStream = file.getInputStream()) {
            upload.setStoragePath(
                    storageService.storeFile(
                            uploadId,
                            fileName,
                            uploadDate,
                            size,
                            inputStream
                    )
            );
        } catch (IOException exception) {
            if (upload.getStoragePath() != null) {
                try {
                    storageService.deleteFile(upload.getStoragePath());
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                    logger.error(
                            "Failed to clean up file after input stream failure: uploadId={}",
                            uploadId,
                            cleanupException
                    );
                }
            }

            throw new StorageException(
                    "Failed to read uploaded file",
                    exception
            );
        }

        try {
            Upload savedUpload =
                    uploadPersistenceService.persistUploadAndOutbox(upload);

            logger.info(
                    "Upload stored and processing event registered: uploadId={}",
                    savedUpload.getUploadId()
            );

            return new UploadResponse(
                    UploadStatus.PENDING,
                    savedUpload.getUploadId()
            );
        } catch (RuntimeException persistenceException) {
            try {
                storageService.deleteFile(upload.getStoragePath());
            } catch (RuntimeException cleanupException) {
                logger.error(
                        "Failed to delete file after persistence failure: uploadId={}",
                        uploadId,
                        cleanupException
                );
                persistenceException.addSuppressed(cleanupException);
            }

            throw persistenceException;
        }
    }
}
