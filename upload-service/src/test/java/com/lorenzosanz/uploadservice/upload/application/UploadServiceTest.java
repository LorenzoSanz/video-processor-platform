package com.lorenzosanz.uploadservice.upload.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.upload.api.UploadResponse;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.upload.exception.FileValidationException;
import com.lorenzosanz.uploadservice.upload.exception.StorageException;
import com.lorenzosanz.uploadservice.upload.application.port.StorageService;
import com.lorenzosanz.uploadservice.upload.application.validation.FileSignatureValidator;
import com.lorenzosanz.uploadservice.upload.application.validation.FileValidator;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    private static final String STORAGE_PATH = "2026/10/test.gif";

    @Mock
    private MultipartFile file;

    @Mock
    private UploadPersistenceService uploadPersistenceService;

    @Mock
    private StorageService storageService;

    @Mock
    private FileValidator fileValidator;

    @Mock
    private FileSignatureValidator fileSignatureValidator;

    @InjectMocks
    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        when(file.getOriginalFilename()).thenReturn("image.gif");
        when(file.getContentType()).thenReturn("image/gif");
        when(file.getSize()).thenReturn(20_000L);
    }

    @Test
    void shouldStartUploadSuccessfully() throws IOException {
        when(file.getInputStream()).thenReturn(
                new ByteArrayInputStream(new byte[]{1, 2, 3})
        );

        when(storageService.storeFile(
                any(UUID.class),
                anyString(),
                any(Instant.class),
                anyLong(),
                any(InputStream.class)
        )).thenReturn(STORAGE_PATH);

        when(uploadPersistenceService.persistUploadAndOutbox(any(Upload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UploadResponse response = uploadService.startUpload(file);

        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getUploadId());

        verify(fileValidator).validate(file);
        verify(fileSignatureValidator).validate(file);

        verify(storageService).storeFile(
                eq(response.getUploadId()),
                eq("image.gif"),
                any(Instant.class),
                eq(20_000L),
                any(InputStream.class)
        );

        ArgumentCaptor<Upload> uploadCaptor =
                ArgumentCaptor.forClass(Upload.class);

        verify(uploadPersistenceService)
                .persistUploadAndOutbox(uploadCaptor.capture());

        assertEquals(
                response.getUploadId(),
                uploadCaptor.getValue().getUploadId()
        );

        assertEquals(
                STORAGE_PATH,
                uploadCaptor.getValue().getStoragePath()
        );
    }

    @Test
    void shouldDeleteStoredFileWhenRepositoryFails() throws IOException {
        when(file.getInputStream()).thenReturn(
                new ByteArrayInputStream(new byte[]{1, 2, 3})
        );

        when(storageService.storeFile(
                any(UUID.class),
                anyString(),
                any(Instant.class),
                anyLong(),
                any(InputStream.class)
        )).thenReturn(STORAGE_PATH);

        when(uploadPersistenceService.persistUploadAndOutbox(any(Upload.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class,
                () -> uploadService.startUpload(file)
        );

        verify(uploadPersistenceService)
                .persistUploadAndOutbox(any(Upload.class));

        verify(storageService).deleteFile(STORAGE_PATH);
    }

    @Test
    void shouldNotSaveUploadWhenStorageFails() throws IOException {
        when(file.getInputStream()).thenReturn(
                new ByteArrayInputStream(new byte[]{1, 2, 3})
        );

        StorageException exception = new StorageException("Storage error");

        doThrow(exception).when(storageService).storeFile(
                any(UUID.class),
                anyString(),
                any(Instant.class),
                anyLong(),
                any(InputStream.class)
        );

        assertThrows(
                StorageException.class,
                () -> uploadService.startUpload(file)
        );

        verify(uploadPersistenceService, never())
                .persistUploadAndOutbox(any(Upload.class));
    }

    @Test
    void shouldStopProcessWhenValidationFails() {
        FileValidationException exception =
                new FileValidationException("File validation exception");

        doThrow(exception)
                .when(fileValidator)
                .validate(any(MultipartFile.class));

        assertThrows(
                FileValidationException.class,
                () -> uploadService.startUpload(file)
        );

        verify(fileSignatureValidator, never()).validate(file);

        verify(storageService, never()).storeFile(
                any(UUID.class),
                anyString(),
                any(Instant.class),
                anyLong(),
                any(InputStream.class)
        );

        verify(uploadPersistenceService, never())
                .persistUploadAndOutbox(any(Upload.class));
    }

    @Test
    void shouldStopProcessWhenSignatureValidationFails() {
        FileValidationException exception =
                new FileValidationException("File signature validation exception");

        doThrow(exception)
                .when(fileSignatureValidator)
                .validate(any(MultipartFile.class));

        assertThrows(
                FileValidationException.class,
                () -> uploadService.startUpload(file)
        );

        verify(fileValidator).validate(file);

        verify(storageService, never()).storeFile(
                any(UUID.class),
                anyString(),
                any(Instant.class),
                anyLong(),
                any(InputStream.class)
        );

        verify(uploadPersistenceService, never())
                .persistUploadAndOutbox(any(Upload.class));
    }
}