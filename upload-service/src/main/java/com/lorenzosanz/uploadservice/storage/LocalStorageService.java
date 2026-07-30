package com.lorenzosanz.uploadservice.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class LocalStorageService implements StorageService {

  @Value("${storage.root-path}")
  private String rootPath;

  private Path finalPath = Path.of(rootPath, storagePath);

  @Override
  public void storeFile(String storagePath, byte[] data) {
    // Implementation for storing file locally

    
  }

  @Override
  public void deleteFile(String storagePath) {
    // Implementation for removing file locally
  }
}
