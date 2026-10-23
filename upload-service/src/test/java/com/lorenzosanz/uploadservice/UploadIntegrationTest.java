package com.lorenzosanz.uploadservice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.lorenzosanz.uploadservice.dto.upload.UploadResponse;
import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.entity.ProcessingJob;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.repository.OutboxEventRepository;
import com.lorenzosanz.uploadservice.repository.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;
import com.lorenzosanz.uploadservice.storage.StorageService;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfiguration.class)
class UploadIntegrationTest {

    private static final Path STORAGE_ROOT = createStorageRoot();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private StorageService storageService;

    private MockMultipartFile file;

    private byte[] fileContent;

    private String fileName;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "storage.root-path",
                () -> STORAGE_ROOT.toString()
        );
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("upload-integration-test-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void cleanStorageRoot() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(STORAGE_ROOT)) {
            for (Path path : paths
                    .sorted(Comparator.reverseOrder())
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @BeforeEach
    void setUp() {
        fileContent = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        fileName = "test-" + UUID.randomUUID() + ".png";

        file = new MockMultipartFile(
                "file",
                fileName,
                "image/png",
                fileContent
        );
    }

    @Test
    void shouldUploadFileAndPersistProcessingResources() throws Exception {
        try {
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

            UUID uploadId = response.getUploadId();

            assertNotNull(uploadId);

            Upload savedUpload = uploadRepository.findById(uploadId)
                    .orElseThrow();

            assertEquals(fileName, savedUpload.getFileName());
            assertEquals("image/png", savedUpload.getContentType());
            assertEquals(fileContent.length, savedUpload.getSize());
            assertEquals(UploadStatus.PENDING, savedUpload.getStatus());
            assertNotNull(savedUpload.getStoragePath());

            try (InputStream storedFile =
                         storageService.openForRead(savedUpload.getStoragePath())) {

                assertArrayEquals(fileContent, storedFile.readAllBytes());
            }

            ProcessingJob job = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertNotNull(job.getJobId());
            assertEquals(uploadId, job.getUploadId());
            assertEquals(ProcessingJobStatus.PENDING, job.getStatus());
            assertEquals(0, job.getAttempts());
            assertNotNull(job.getCreatedAt());
            assertNotNull(job.getNextAttemptAt());
            assertNull(job.getClaimToken());
            assertNull(job.getLeaseUntil());
            assertNull(job.getLastHeartbeatAt());
            assertNull(job.getOutputPath());

            List<OutboxEvent> events = outboxEventRepository.findAll()
                    .stream()
                    .filter(event -> uploadId.equals(event.getAggregateId()))
                    .toList();

            assertEquals(1, events.size());

            OutboxEvent event = events.get(0);

            assertNotNull(event.getEventId());
            assertEquals("UPLOAD", event.getAggregateType());
            assertEquals(uploadId, event.getAggregateId());
            assertEquals(
                    "VIDEO_PROCESSING_REQUESTED",
                    event.getEventType()
            );
            assertTrue(event.getPayload().contains(uploadId.toString()));
            assertNotNull(event.getCreatedAt());
            assertNull(event.getPublishedAt());
            assertEquals(0, event.getAttempts());

        } finally {
            cleanUploadResources();
        }
    }

    private void cleanUploadResources() {
        List<Upload> uploadsToClean = uploadRepository.findAll()
                .stream()
                .filter(upload -> fileName.equals(upload.getFileName()))
                .toList();

        List<String> storagePaths = uploadsToClean.stream()
                .map(Upload::getStoragePath)
                .filter(Objects::nonNull)
                .toList();

        for (Upload upload : uploadsToClean) {
            UUID uploadId = upload.getUploadId();

            outboxEventRepository.findAll()
                    .stream()
                    .filter(event -> uploadId.equals(event.getAggregateId()))
                    .map(OutboxEvent::getEventId)
                    .toList()
                    .forEach(outboxEventRepository::deleteById);

            processingJobRepository.findByUploadId(uploadId)
                    .ifPresent(job ->
                            processingJobRepository.deleteById(job.getJobId())
                    );

            uploadRepository.deleteById(uploadId);
        }

        storagePaths.forEach(storageService::deleteFile);
    }
}