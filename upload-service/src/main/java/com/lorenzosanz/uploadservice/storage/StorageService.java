package com.lorenzosanz.uploadservice.storage;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

public interface StorageService {

    /**
     * Stores exactly {@code expectedSize} bytes and returns the relative storage path.
     *
     * <p>The caller owns the input stream and is responsible for closing it.
     * Implementations must not close the supplied input stream.</p>
     *
     * @throws com.lorenzosanz.uploadservice.exception.StorageException
     *         if the stream cannot be stored or its byte count differs from expectedSize
     */
    String storeFile(
            UUID uploadId,
            String originalFilename,
            Instant createdAt,
            long expectedSize,
            InputStream inputStream
    );

    void deleteFile(String storagePath);
}
