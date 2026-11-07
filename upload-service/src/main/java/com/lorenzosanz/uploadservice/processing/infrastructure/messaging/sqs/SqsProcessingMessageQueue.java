
package com.lorenzosanz.uploadservice.processing.infrastructure.messaging.sqs;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.lorenzosanz.uploadservice.processing.application.port.ProcessingMessageQueue;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@ConditionalOnProperty(
        prefix = "messaging.sqs",
        name = "enabled",
        havingValue = "true"
)
public class SqsProcessingMessageQueue implements ProcessingMessageQueue {

    private static final int MAX_MESSAGES = 10;
    private static final long MAX_WAIT_SECONDS = 20;
    private static final long MAX_VISIBILITY_SECONDS = 43200;

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsProcessingMessageQueue(
            SqsClient sqsClient,
            @Value("${messaging.sqs.queue-url:}") String queueUrl) {

        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException(
                    "messaging.sqs.queue-url must be configured when SQS is enabled"
            );
        }

        this.sqsClient = Objects.requireNonNull(
                sqsClient,
                "SqsClient is required"
        );

        this.queueUrl = queueUrl;
    }

    @Override
    public List<ReceivedMessage> receive(
            int maxMessages,
            Duration waitTime,
            Duration visibilityTimeout) {

        if (maxMessages < 1 || maxMessages > MAX_MESSAGES) {
            throw new IllegalArgumentException(
                    "Maximum messages must be between 1 and 10"
            );
        }

        int waitTimeSeconds = toSeconds(
                waitTime,
                "Wait time",
                MAX_WAIT_SECONDS
        );

        int visibilitySeconds = toSeconds(
                visibilityTimeout,
                "Visibility timeout",
                MAX_VISIBILITY_SECONDS
        );

        var response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(waitTimeSeconds)
                        .visibilityTimeout(visibilitySeconds)
                        .build()
        );

        return response.messages()
                .stream()
                .map(this::toReceivedMessage)
                .toList();
    }

    @Override
    public void delete(ReceivedMessage message) {
        Objects.requireNonNull(message, "Message is required");

        sqsClient.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()
        );
    }

    @Override
    public void changeVisibility(
            ReceivedMessage message,
            Duration visibilityTimeout) {

        Objects.requireNonNull(message, "Message is required");

        int visibilitySeconds = toSeconds(
                visibilityTimeout,
                "Visibility timeout",
                MAX_VISIBILITY_SECONDS
        );

        sqsClient.changeMessageVisibility(
                ChangeMessageVisibilityRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .visibilityTimeout(visibilitySeconds)
                        .build()
        );
    }

    private ReceivedMessage toReceivedMessage(Message message) {

        if (message.messageId() == null
                || message.body() == null
                || message.receiptHandle() == null) {
            throw new IllegalStateException(
                    "SQS returned a message with missing required fields"
            );
        }

        return new ReceivedMessage(
                message.messageId(),
                message.body(),
                message.receiptHandle()
        );
    }

    private int toSeconds(
            Duration duration,
            String parameterName,
            long maxSeconds) {

        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException(
                    parameterName + " must not be null or negative"
            );
        }

        if (duration.getNano() != 0) {
            throw new IllegalArgumentException(
                    parameterName + " must use whole seconds"
            );
        }

        long seconds = duration.getSeconds();

        if (seconds > maxSeconds) {
            throw new IllegalArgumentException(
                    parameterName + " cannot exceed "
                            + maxSeconds + " seconds"
            );
        }

        return Math.toIntExact(seconds);
    }
}
