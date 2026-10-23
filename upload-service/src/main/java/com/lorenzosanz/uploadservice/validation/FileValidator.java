package com.lorenzosanz.uploadservice.validation;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.config.StorageProperties;
import com.lorenzosanz.uploadservice.exception.FileValidationException;
import com.lorenzosanz.uploadservice.validation.constants.AllowedContentTypes;

@Service
public class FileValidator {

    private final StorageProperties storageProperties;

    public FileValidator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public void validate(MultipartFile file) {
        if (file == null) {
            throw new FileValidationException("File is required.");
        }

        if (file.isEmpty()) {
            throw new FileValidationException("File is empty.");
        }

        if (file.getSize() > storageProperties.getMaxFileSize()) {
            throw new FileValidationException("File is too big.");
        }

        String contentType = file.getContentType();

        if (contentType == null
                || contentType.isBlank()
                || !AllowedContentTypes.VALUES.contains(
                        contentType.trim().toLowerCase(Locale.ROOT))) {
            throw new FileValidationException(
                    "File content type is not allowed."
            );
        }
    }
}