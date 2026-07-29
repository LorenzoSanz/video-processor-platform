package com.lorenzosanz.uploadservice.storage;

public interface StorageService {

  void storeFile(String path, byte[] fileData);

  void removeFile(String path);
}
