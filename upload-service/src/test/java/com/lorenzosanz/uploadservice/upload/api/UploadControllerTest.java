package com.lorenzosanz.uploadservice.upload.api;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.shared.web.GlobalExceptionHandler;
import com.lorenzosanz.uploadservice.upload.application.UploadService;
import com.lorenzosanz.uploadservice.upload.exception.FileValidationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    private MockMvc mockMvc;

    private MockMultipartFile file;

    private UUID uploadId;

    @Mock
    private UploadService uploadService;

    @InjectMocks
    private UploadController uploadController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(uploadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        file = new MockMultipartFile(
                "file",
                "image.gif",
                MediaType.IMAGE_GIF_VALUE,
                "fake image content".getBytes()
        );

        uploadId = UUID.randomUUID();
    }

    @Test
    void shouldReturnUploadResponseWhenFileIsValid() throws Exception {
        when(uploadService.startUpload(any(MultipartFile.class)))
                .thenReturn(new UploadResponse(
                        "PENDING",
                        uploadId
                ));

        mockMvc.perform(
                multipart("/upload")
                        .file(file)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.uploadId").value(uploadId.toString()));

        verify(uploadService).startUpload(any(MultipartFile.class));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws Exception {

        doThrow(new FileValidationException("Validation failed"))
                .when(uploadService)
                .startUpload(any(MultipartFile.class));

        mockMvc.perform(
                multipart("/upload")
                        .file(file)
        )
                .andExpect(status().isBadRequest());
    }
}