package com.lorenzosanz.uploadservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.lorenzosanz.uploadservice.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.entity.Upload;
import com.lorenzosanz.uploadservice.enums.UploadStatus;
import com.lorenzosanz.uploadservice.messaging.OutboxMessage;
import com.lorenzosanz.uploadservice.repository.OutboxEventRepository;
import com.lorenzosanz.uploadservice.repository.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.repository.UploadRepository;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class OutboxToSqsIntegrationTest {

    private static final int SQS_PORT = 9324;
    private static final String QUEUE_NAME = "outbox-to-sqs-integration";

    @Container
    static final GenericContainer<?> elasticmq =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "softwaremill/elasticmq-native:1.7.1"
                    )
            )
            .withExposedPorts(SQS_PORT)
            .waitingFor(
                    Wait.forHttp("/health")
                            .forPort(SQS_PORT)
                            .forStatusCode(200)
            )
            .withStartupTimeout(Duration.ofSeconds(45));

    private static String queueUrl;

    @Autowired
    private UploadPersistenceService uploadPersistenceService;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("messaging.sqs.enabled", () -> "true");
        registry.add("messaging.sqs.endpoint", OutboxToSqsIntegrationTest::endpoint);
        registry.add("messaging.sqs.queue-url", OutboxToSqsIntegrationTest::getQueueUrl);
        registry.add("messaging.sqs.region", () -> "us-east-1");
        registry.add("messaging.sqs.access-key", () -> "test");
        registry.add("messaging.sqs.secret-key", () -> "test");

        registry.add("outbox.publisher.enabled", () -> "true");
        registry.add("outbox.publisher.scheduling-enabled", () -> "false");
    }

    private static String endpoint() {
        return "http://" + elasticmq.getHost()
                + ":" + elasticmq.getMappedPort(SQS_PORT);
    }

    private static synchronized String getQueueUrl() {
        if (queueUrl == null) {
            try (SqsClient client = SqsClient.builder()
                    .endpointOverride(URI.create(endpoint()))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("test", "test")
                            )
                    )
                    .build()) {

                queueUrl = client.createQueue(
                        CreateQueueRequest.builder()
                                .queueName(QUEUE_NAME)
                                .build()
                ).queueUrl();
            }
        }

        return queueUrl;
    }

    @Test
    void shouldPersistPublishAndMarkOutboxEventAsPublished() throws Exception {
        UUID uploadId = UUID.randomUUID();
        UUID eventId = null;

        try {
            Upload upload = new Upload(
                    uploadId,
                    "integration-video.mp4",
                    UploadStatus.PENDING,
                    "video/mp4",
                    1024L,
                    Instant.now()
            );

            uploadPersistenceService.persistUploadAndOutbox(upload);

            assertThat(uploadRepository.findById(uploadId)).isPresent();

            OutboxEvent persistedEvent = outboxEventRepository.findAll()
                    .stream()
                    .filter(event -> uploadId.equals(event.getAggregateId()))
                    .findFirst()
                    .orElseThrow();

            eventId = persistedEvent.getEventId();

            assertThat(persistedEvent.getPublishedAt()).isNull();

            int publishedCount = outboxPublisherService.publishPending(
                    10,
                    Duration.ofSeconds(30)
            );

            assertThat(publishedCount).isEqualTo(1);

            OutboxEvent publishedEvent = outboxEventRepository
                    .findById(eventId)
                    .orElseThrow();

            assertThat(publishedEvent.getPublishedAt()).isNotNull();
            assertThat(publishedEvent.getAttempts()).isZero();
            assertThat(publishedEvent.getLastError()).isNull();

            var response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(1)
                            .waitTimeSeconds(2)
                            .build()
            );

            assertThat(response.messages()).hasSize(1);

            Message received = response.messages().get(0);

            try {
                OutboxMessage actual = jsonMapper.readValue(
                        received.body(),
                        OutboxMessage.class
                );

                assertThat(actual.eventId()).isEqualTo(eventId);
                assertThat(actual.aggregateType()).isEqualTo("UPLOAD");
                assertThat(actual.aggregateId()).isEqualTo(uploadId);
                assertThat(actual.eventType())
                        .isEqualTo("VIDEO_PROCESSING_REQUESTED");
                assertThat(actual.payload())
                        .isEqualTo("{\"uploadId\":\"" + uploadId + "\"}");
            } finally {
                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(received.receiptHandle())
                                .build()
                );
            }

        } finally {
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
    
                if (uploadRepository.existsById(uploadId)) {
                    uploadRepository.deleteById(uploadId);
                }
            }
    }
}