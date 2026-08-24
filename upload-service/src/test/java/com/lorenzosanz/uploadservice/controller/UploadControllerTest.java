package com.lorenzosanz.uploadservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.lorenzosanz.uploadservice.dto.upload.UploadResponse;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.exception.FileValidationException;
import com.lorenzosanz.uploadservice.exception.GlobalExceptionHandler;
import com.lorenzosanz.uploadservice.service.UploadService;

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
      "image/gif",
      "fake image content".getBytes()
  );

    uploadId = UUID.randomUUID();
  }

  @Test 
  void shouldReturnUploadResponseWhenFileIsValid() throws Exception {
    when(uploadService.startUpload(any(MultipartFile.class)))
    .thenReturn(new UploadResponse(
      UploadStatus.PENDING,
      uploadId
    ));
    
    mockMvc.perform(
      multipart("/upload")
        .file(file))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("PENDING"));

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
