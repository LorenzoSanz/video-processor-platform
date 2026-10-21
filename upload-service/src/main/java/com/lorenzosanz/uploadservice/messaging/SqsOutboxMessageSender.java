package com.lorenzosanz.uploadservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Component
@ConditionalOnProperty(
        prefix = "messaging.sqs",
        name = "enabled",
        havingValue = "true"
)
public class SqsOutboxMessageSender implements OutboxMessageSender {

    private static final Logger logger =
            LoggerFactory.getLogger(SqsOutboxMessageSender.class);

    private final SqsClient sqsClient;
    private final JsonMapper jsonMapper;
    private final String queueUrl;

    public SqsOutboxMessageSender(
            SqsClient sqsClient,
            JsonMapper jsonMapper,
            @Value("${messaging.sqs.queue-url:}") String queueUrl) {

        if (queueUrl.isBlank()) {
            throw new IllegalStateException(
                    "messaging.sqs.queue-url must be configured when SQS is enabled"
            );
        }

        this.sqsClient = sqsClient;
        this.jsonMapper = jsonMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void send(OutboxMessage message) {
        final String body;

        try {
            body = jsonMapper.writeValueAsString(message);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not serialize outbox event " + message.eventId(),
                    exception
            );
        }

        SendMessageResponse response = sqsClient.sendMessage(
                SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(body)
                        .build()
        );

        logger.debug(
                "Published outbox event {} as SQS message {}",
                message.eventId(),
                response.messageId()
        );
    }
}