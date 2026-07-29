package com.lorenzosanz.uploadservice.storage;

import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class LocalStorageService implements StorageService {

  @Value("${storage.root-path}")
  private String rootPath;

  public void storeFile(String path, byte[] fileData) {
    // Implementation for storing file locally
    Upload upload = new Upload();
    rootPath = upload.getUploadId().toString();
    
  }

  public void removeFile(String path) {
    // Implementation for removing file locally
  }
}
