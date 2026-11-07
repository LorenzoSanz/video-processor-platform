
package com.lorenzosanz.uploadservice.processing.integration;

import com.lorenzosanz.uploadservice.outbox.application.OutboxMessage;
import com.lorenzosanz.uploadservice.processing.infrastructure.messaging.sqs.ProcessingMessageParser;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessingMessageParserTest {

    private static final UUID EVENT_ID =
            UUID.fromString("808dd3f9-aac5-4527-8c54-7503e1a72fa0");

    private static final UUID UPLOAD_ID =
            UUID.fromString("7ddf4da9-164a-4f0f-9d62-6462ba2265aa");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final ProcessingMessageParser parser =
            new ProcessingMessageParser(jsonMapper);

    @Test
    void shouldParseValidProcessingMessage() throws Exception {
        OutboxMessage result = parser.parse(
                createMessage(
                        EVENT_ID,
                        "UPLOAD",
                        UPLOAD_ID,
                        "VIDEO_PROCESSING_REQUESTED",
                        createPayload(UPLOAD_ID)
                )
        );

        assertEquals(EVENT_ID, result.eventId());
        assertEquals("UPLOAD", result.aggregateType());
        assertEquals(UPLOAD_ID, result.aggregateId());
        assertEquals("VIDEO_PROCESSING_REQUESTED", result.eventType());
        assertEquals(createPayload(UPLOAD_ID), result.payload());
    }

    @Test
    void shouldRejectNullMessageBody() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(null)
        );
    }

    @Test
    void shouldRejectEmptyMessageBody() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("")
        );
    }

    @Test
    void shouldRejectBlankMessageBody() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("   ")
        );
    }

    @Test
    void shouldRejectMalformedMessageJson() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("{invalid-json")
        );
    }

    @Test
    void shouldRejectMessageWithoutEventId() throws Exception {
        String message = createMessage(
                null,
                "UPLOAD",
                UPLOAD_ID,
                "VIDEO_PROCESSING_REQUESTED",
                createPayload(UPLOAD_ID)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(message)
        );
    }

    @Test
    void shouldRejectUnsupportedAggregateType() throws Exception {
        String message = createMessage(
                EVENT_ID,
                "VIDEO",
                UPLOAD_ID,
                "VIDEO_PROCESSING_REQUESTED",
                createPayload(UPLOAD_ID)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(message)
        );
    }

    @Test
    void shouldRejectUnsupportedEventType() throws Exception {
        String message = createMessage(
                EVENT_ID,
                "UPLOAD",
                UPLOAD_ID,
                "VIDEO_DELETED",
                createPayload(UPLOAD_ID)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(message)
        );
    }

    @Test
    void shouldRejectEmptyPayload() throws Exception {
        String message = createMessage(
                EVENT_ID,
                "UPLOAD",
                UPLOAD_ID,
                "VIDEO_PROCESSING_REQUESTED",
                ""
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(message)
        );
    }

    @Test
    void shouldRejectMalformedPayloadJson() throws Exception {
        String message = createMessage(
                EVENT_ID,
                "UPLOAD",
                UPLOAD_ID,
                "VIDEO_PROCESSING_REQUESTED",
                "{invalid-payload"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(message)
        );
    }

    @Test
    void shouldRejectPayloadWithoutUploadId() throws Exception {
        String message = createMessage(
                EVENT_ID,
                "UPLOAD",
                UPLOAD_ID,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"otherId\":\"" + UPLOAD_ID + "\"}"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(message)
        );
    }

    @Test
    void shouldRejectPayloadWithDifferentUploadId() throws Exception {
        UUID differentUploadId = UUID.randomUUID();

        String message = createMessage(
                EVENT_ID,
                "UPLOAD",
                UPLOAD_ID,
                "VIDEO_PROCESSING_REQUESTED",
                createPayload(differentUploadId)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(message)
        );
    }

    private String createMessage(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload) throws Exception {

        OutboxMessage message = new OutboxMessage(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                payload
        );

        return jsonMapper.writeValueAsString(message);
    }

    private String createPayload(UUID uploadId) {
        return "{\"uploadId\":\"" + uploadId + "\"}";
    }
}
