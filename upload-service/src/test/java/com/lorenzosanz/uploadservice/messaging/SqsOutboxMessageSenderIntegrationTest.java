package com.lorenzosanz.uploadservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
class SqsOutboxMessageSenderIntegrationTest {

    private static final int SQS_PORT = 9324;

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

    private static SqsClient sqsClient;
    private static JsonMapper jsonMapper;
    private static String queueUrl;
    private static SqsOutboxMessageSender sender;

    @BeforeAll
    static void setUp() {
        URI endpoint = URI.create(
                "http://" + elasticmq.getHost()
                        + ":" + elasticmq.getMappedPort(SQS_PORT)
        );

        sqsClient = SqsClient.builder()
                .endpointOverride(endpoint)
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        "test",
                                        "test"
                                )
                        )
                )
                .build();

        queueUrl = sqsClient.createQueue(
                CreateQueueRequest.builder()
                        .queueName(
                                "video-processing-it-" + UUID.randomUUID()
                        )
                        .build()
        ).queueUrl();

        jsonMapper = JsonMapper.builder().build();

        sender = new SqsOutboxMessageSender(
                sqsClient,
                jsonMapper,
                queueUrl
        );
    }

    @AfterAll
    static void tearDown() {
        if (sqsClient != null) {
            sqsClient.close();
        }
    }

    @Test
    void shouldSendOutboxMessageAndPreserveItsFields() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();

        OutboxMessage expected = new OutboxMessage(
                eventId,
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"uploadId\":\"" + uploadId + "\"}"
        );

        sender.send(expected);

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

            assertThat(actual).isEqualTo(expected);
        } finally {
            sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(received.receiptHandle())
                            .build()
            );
        }
    }
}