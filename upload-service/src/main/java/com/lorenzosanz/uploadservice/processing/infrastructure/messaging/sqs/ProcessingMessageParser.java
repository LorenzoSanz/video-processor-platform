
package com.lorenzosanz.uploadservice.processing.infrastructure.messaging.sqs;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lorenzosanz.uploadservice.outbox.application.OutboxMessage;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProcessingMessageParser {

    private static final String EXPECTED_AGGREGATE_TYPE = "UPLOAD";

    private static final String EXPECTED_EVENT_TYPE =
            "VIDEO_PROCESSING_REQUESTED";

    private final JsonMapper jsonMapper;

    public ProcessingMessageParser(JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "JsonMapper is required"
        );
    }

    public OutboxMessage parse(String messageBody) {

        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException(
                    "SQS message body must not be empty"
            );
        }

        final OutboxMessage message;

        try {
            message = jsonMapper.readValue(
                    messageBody,
                    OutboxMessage.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Could not deserialize SQS processing message",
                    exception
            );
        }

        if (message == null) {
            throw new IllegalArgumentException(
                    "SQS message must not be null"
            );
        }

        validateEnvelope(message);

        final VideoProcessingRequestedPayload payload;

        try {
            payload = jsonMapper.readValue(
                    message.payload(),
                    VideoProcessingRequestedPayload.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Could not deserialize processing event payload",
                    exception
            );
        }

        if (payload == null || payload.uploadId() == null) {
            throw new IllegalArgumentException(
                    "Processing payload must contain a valid uploadId"
            );
        }

        if (!message.aggregateId().equals(payload.uploadId())) {
            throw new IllegalArgumentException(
                    "Payload uploadId does not match aggregateId"
            );
        }

        return message;
    }

    private void validateEnvelope(OutboxMessage message) {

        if (message.eventId() == null) {
            throw new IllegalArgumentException(
                    "Event ID is required"
            );
        }

        if (!EXPECTED_AGGREGATE_TYPE.equals(message.aggregateType())) {
            throw new IllegalArgumentException(
                    "Unsupported aggregate type: "
                            + message.aggregateType()
            );
        }

        if (message.aggregateId() == null) {
            throw new IllegalArgumentException(
                    "Aggregate ID is required"
            );
        }

        if (!EXPECTED_EVENT_TYPE.equals(message.eventType())) {
            throw new IllegalArgumentException(
                    "Unsupported event type: "
                            + message.eventType()
            );
        }

        if (message.payload() == null || message.payload().isBlank()) {
            throw new IllegalArgumentException(
                    "Event payload must not be empty"
            );
        }
    }

    public record VideoProcessingRequestedPayload(UUID uploadId) {
    }
}
