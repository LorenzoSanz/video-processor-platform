
package com.lorenzosanz.uploadservice.processing.infrastructure.messaging.sqs;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.lorenzosanz.uploadservice.processing.application.port.ProcessingMessageQueue.ReceivedMessage;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class SqsProcessingMessageQueueIntegrationTest {

    private static final int SQS_PORT = 9324;

    private static final Duration WAIT_TIME =
            Duration.ofSeconds(2);

    private static final Duration VISIBILITY_TIMEOUT =
            Duration.ofSeconds(30);

    @Container
    @SuppressWarnings("resource")
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

    private SqsProcessingMessageQueue processingMessageQueue;

    private String queueUrl;

    @BeforeAll
    static void setUpClient() {
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
    }

    @BeforeEach
    void setUpQueue() {
        queueUrl = sqsClient.createQueue(
                CreateQueueRequest.builder()
                        .queueName(
                                "processing-queue-it-" + UUID.randomUUID()
                        )
                        .build()
        ).queueUrl();

        processingMessageQueue = new SqsProcessingMessageQueue(
                sqsClient,
                queueUrl
        );
    }

    @AfterEach
    void tearDownQueue() {
        if (queueUrl != null) {
            sqsClient.deleteQueue(
                    DeleteQueueRequest.builder()
                            .queueUrl(queueUrl)
                            .build()
            );
        }
    }

    @AfterAll
    static void tearDownClient() {
        if (sqsClient != null) {
            sqsClient.close();
        }
    }

    @Test
    void shouldReceiveMessageAndPreserveItsFields() {
        String body = "processing-message-" + UUID.randomUUID();

        sendMessage(body);

        List<ReceivedMessage> messages =
                processingMessageQueue.receive(
                        1,
                        WAIT_TIME,
                        VISIBILITY_TIMEOUT
                );

        assertEquals(1, messages.size());

        ReceivedMessage received = messages.get(0);

        assertNotNull(received.messageId());
        assertEquals(body, received.body());
        assertNotNull(received.receiptHandle());
        assertFalse(received.receiptHandle().isBlank());
    }

    @Test
    void shouldDeleteReceivedMessage() {
        String body = "delete-message-" + UUID.randomUUID();

        sendMessage(body);

        List<ReceivedMessage> messages =
                processingMessageQueue.receive(
                        1,
                        WAIT_TIME,
                        VISIBILITY_TIMEOUT
                );

        assertEquals(1, messages.size());

        processingMessageQueue.delete(messages.get(0));

        List<ReceivedMessage> remainingMessages =
                processingMessageQueue.receive(
                        1,
                        WAIT_TIME,
                        VISIBILITY_TIMEOUT
                );

        assertTrue(remainingMessages.isEmpty());
    }

    @Test
    void shouldMakeReceivedMessageVisibleAgain() {
        String body = "visibility-message-" + UUID.randomUUID();

        sendMessage(body);

        List<ReceivedMessage> firstDelivery =
                processingMessageQueue.receive(
                        1,
                        WAIT_TIME,
                        VISIBILITY_TIMEOUT
                );

        assertEquals(1, firstDelivery.size());

        ReceivedMessage firstMessage = firstDelivery.get(0);

        processingMessageQueue.changeVisibility(
                firstMessage,
                Duration.ZERO
        );

        List<ReceivedMessage> secondDelivery =
                processingMessageQueue.receive(
                        1,
                        WAIT_TIME,
                        VISIBILITY_TIMEOUT
                );

        assertEquals(1, secondDelivery.size());

        ReceivedMessage redeliveredMessage = secondDelivery.get(0);

        assertEquals(body, redeliveredMessage.body());

        assertNotEquals(
                firstMessage.receiptHandle(),
                redeliveredMessage.receiptHandle()
        );

        // Use the receipt handle from the latest delivery.
        processingMessageQueue.delete(redeliveredMessage);
    }

    private void sendMessage(String body) {
        sqsClient.sendMessage(
                SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(body)
                        .build()
        );
    }
}
