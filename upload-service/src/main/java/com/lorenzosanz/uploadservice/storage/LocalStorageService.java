package com.lorenzosanz.uploadservice.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.exception.StorageException;
import java.nio.file.StandardOpenOption;

@Service
public class LocalStorageService implements StorageService {

  @Value("${storage.root-path}")
  private String rootPath;

  private String buildStoragePath(Upload upload) {
      final LocalDate date = upload.getDate();
      final int year = date.getYear();
      final String month = String.format("%02d", date.getMonthValue());
      

      final String fileName = upload.getFileName();
      final String extension = fileName.substring(fileName.lastIndexOf("."));
      // TODO: Validate filenames without extension.

      return year + "/" + month + "/" + upload.getUploadId() + extension;
  }

  @Override
  public void storeFile(Upload upload,  MultipartFile file) {
    try {
      final String storagePath = buildStoragePath(upload);

      final Path filePath = Path.of(rootPath, storagePath);

      Files.createDirectories(filePath.getParent());
      Files.write(
                  filePath,
                  file.getBytes(),
                  StandardOpenOption.CREATE_NEW
      );
      
      upload.setStoragePath(storagePath);

    } catch (IOException e) {
    throw new StorageException("Failed to store file", e);
    }
  }

  
  @Override
  public void deleteFile(String storagePath) {
    try {
      final Path filePath = Path.of(rootPath, storagePath);
      Files.deleteIfExists(filePath);

    } catch (IOException e) {
      throw new StorageException("Failed to delete file", e);
    }
  }
}
