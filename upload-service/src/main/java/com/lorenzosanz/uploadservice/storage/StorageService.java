package com.lorenzosanz.uploadservice.storage;

import com.lorenzosanz.uploadservice.entity.Upload; 
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  void storeFile(Upload upload, MultipartFile multipartFile);

  void deleteFile(String storagePath);
}
