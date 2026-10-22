package com.lorenzosanz.uploadservice.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lorenzosanz.uploadservice.config.StorageProperties;
import com.lorenzosanz.uploadservice.exception.StorageException;

@Service
public class LocalStorageService implements StorageService {

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private final StorageProperties storageProperties;

    public LocalStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String storeFile(
            UUID uploadId,
            String originalFilename,
            Instant createdAt,
            long expectedSize,
            InputStream inputStream) {

        if (uploadId == null || createdAt == null || inputStream == null) {
            throw new IllegalArgumentException(
                    "uploadId, createdAt and inputStream are required"
            );
        }
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize must not be negative");
        }

        String storagePath = buildStoragePath(
                uploadId,
                originalFilename,
                createdAt
        );
        Path filePath = resolveStoragePath(storagePath);
        boolean fileCreated = false;

        try {
            Files.createDirectories(filePath.getParent());

            try (OutputStream outputStream = Files.newOutputStream(
                    filePath,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {

                // This prevents cleanup from deleting a pre-existing file.
                fileCreated = true;

                byte[] buffer = new byte[COPY_BUFFER_SIZE];
                long bytesCopied = 0;
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (bytesRead == 0) {
                        // To non-standard InputStreams that return zero
                        // for a non-empty buffer, without spinning indefinitely.
                        int singleByte = inputStream.read();
                        if (singleByte == -1) {
                            break;
                        }
                        if (bytesCopied >= expectedSize) {
                            throw new IOException(
                                    "Input stream contains more bytes than expected"
                            );
                        }
                        outputStream.write(singleByte);
                        bytesCopied++;
                        continue;
                    }

                    if (bytesRead > expectedSize - bytesCopied) {
                        throw new IOException(
                                "Input stream contains more bytes than expected"
                        );
                    }

                    outputStream.write(buffer, 0, bytesRead);
                    bytesCopied += bytesRead;
                }

                if (bytesCopied != expectedSize) {
                    throw new IOException(
                            "Expected " + expectedSize + " bytes but read " + bytesCopied
                    );
                }
            }

            return storagePath;

        } catch (IOException exception) {
            if (fileCreated) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            throw new StorageException("Failed to store file", exception);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        Path filePath = resolveStoragePath(storagePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new StorageException("Failed to delete file", exception);
        }
    }

    private String buildStoragePath(
            UUID uploadId,
            String originalFilename,
            Instant createdAt) {

        var utcDate = createdAt.atZone(ZoneOffset.UTC);
        int year = utcDate.getYear();
        String month = String.format(
                Locale.ROOT,
                "%02d",
                utcDate.getMonthValue()
        );

        return year + "/" + month + "/" + uploadId
                + extractSafeExtension(originalFilename);
    }

    private String extractSafeExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }

        String candidate = originalFilename.substring(lastDot);
        if (!candidate.matches("\\.[A-Za-z0-9]{1,10}")) {
            return "";
        }

        return candidate.toLowerCase(Locale.ROOT);
    }

    private Path resolveStoragePath(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new StorageException("Storage path must not be empty");
        }

        Path root = Path.of(storageProperties.getRootPath())
                .toAbsolutePath()
                .normalize();
        Path resolved = root.resolve(storagePath).normalize();

        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new StorageException(
                    "Storage path resolves outside the storage root"
            );
        }

        return resolved;
    }

    @Override
    public InputStream openForRead(String storagePath) {
    Path filePath = resolveStoragePath(storagePath);

    try {
        return Files.newInputStream(
                filePath,
                StandardOpenOption.READ
        );
    } catch (IOException exception) {
        throw new StorageException(
                "Failed to open stored file for reading",
                exception
        );
    }
}
}
