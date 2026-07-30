package com.lorenzosanz.uploadservice.storage;

public interface StorageService {

  String storeFile(String storagePath, byte[] data);

  void deleteFile(String storagePath);
}
