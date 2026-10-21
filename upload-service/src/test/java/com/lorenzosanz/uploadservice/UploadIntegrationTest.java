package com.lorenzosanz.uploadservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.lorenzosanz.uploadservice.dto.upload.UploadResponse;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;

import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfiguration.class)
class UploadIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  
  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UploadRepository uploadRepository;

  private MockMultipartFile file;

  byte[] fileContent;



  @BeforeEach
  void setUp() {
    fileContent = new byte[] {
      (byte) 0x89, 0x50, 0x4E, 0x47,
      0x0D, 0x0A, 0x1A, 0x0A
    };

    file = new MockMultipartFile(
        "file",
        "test.png",
        "image/png",
        fileContent
    );
  } 
  
  @Test
  void shouldLoadApplicationContext() {
  }

  @Test
  void shouldUploadFile() throws Exception {
    MvcResult result = mockMvc.perform(
      multipart("/upload")
        .file(file)
    )
    .andExpect(status().isOk())
    .andReturn();

    UploadResponse response = objectMapper.readValue(
    result.getResponse().getContentAsString(),
    UploadResponse.class
);

  Optional<Upload> savedUpload = uploadRepository.findById(response.getUploadId());
  
  assertTrue(savedUpload.isPresent());

  assertEquals("test.png", savedUpload.get().getFileName());
  assertEquals("image/png", savedUpload.get().getContentType());
  assertEquals(fileContent.length, savedUpload.get().getSize());
  assertEquals(UploadStatus.PENDING, savedUpload.get().getStatus());
  }

}
