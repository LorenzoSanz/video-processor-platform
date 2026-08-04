package com.lorenzosanz.uploadservice.validation;

import com.lorenzosanz.uploadservice.config.StorageProperties;
import com.lorenzosanz.uploadservice.exception.FileValidationException;
import com.lorenzosanz.uploadservice.validation.constants.AllowedContentTypes;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidator {

  private final StorageProperties storageProperties;

  public FileValidator(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  public void validate(MultipartFile file) {

    if (file.isEmpty()) {
      throw new FileValidationException("File is empty.");
    }

    if (file.getSize() > storageProperties.getMaxFileSize()) {
      throw new FileValidationException("File is too big.");
    }

    if (!AllowedContentTypes.VALUES.contains(file.getContentType())) {
      throw new FileValidationException("File content type is not allowed.");
    }
  }
}
