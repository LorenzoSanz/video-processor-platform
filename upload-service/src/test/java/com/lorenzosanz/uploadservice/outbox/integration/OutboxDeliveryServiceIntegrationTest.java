package com.lorenzosanz.uploadservice.outbox.integration;
import com.lorenzosanz.uploadservice.outbox.application.OutboxDeliveryService;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.outbox.domain.OutboxEvent;
import com.lorenzosanz.uploadservice.outbox.infrastructure.persistence.OutboxEventRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class OutboxDeliveryServiceIntegrationTest {

    @Autowired
    private OutboxDeliveryService deliveryService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldMarkPublishedWhenClaimTokenMatches() {
        UUID uploadId = UUID.randomUUID();
        UUID token = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"uploadId\":\"" + uploadId + "\"}"
        );

        event.claim(token, Instant.now().plusSeconds(120));

        OutboxEvent saved = outboxEventRepository.saveAndFlush(event);

        try {
            assertTrue(deliveryService.markPublished(
                    saved.getEventId(), token));

            OutboxEvent persisted = outboxEventRepository
                    .findById(saved.getEventId())
                    .orElseThrow();

            assertNotNull(persisted.getPublishedAt());
            assertNull(persisted.getClaimToken());
            assertNull(persisted.getClaimedUntil());
        } finally {
            outboxEventRepository.deleteById(saved.getEventId());
        }
    }

    @Test
    void shouldRejectStaleClaimToken() {
        UUID uploadId = UUID.randomUUID();
        UUID currentToken = UUID.randomUUID();
        UUID staleToken = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"uploadId\":\"" + uploadId + "\"}"
        );

        event.claim(currentToken, Instant.now().plusSeconds(120));

        OutboxEvent saved = outboxEventRepository.saveAndFlush(event);

        try {
            assertFalse(deliveryService.markPublished(
                    saved.getEventId(), staleToken));

            OutboxEvent persisted = outboxEventRepository
                    .findById(saved.getEventId())
                    .orElseThrow();

            assertNull(persisted.getPublishedAt());
            assertEquals(currentToken, persisted.getClaimToken());
        } finally {
            outboxEventRepository.deleteById(saved.getEventId());
        }
    }

    @Test
    void shouldScheduleRetryAndReleaseClaimAfterFailure() {
        UUID uploadId = UUID.randomUUID();
        UUID token = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"uploadId\":\"" + uploadId + "\"}"
        );

        event.claim(token, Instant.now().plusSeconds(120));

        OutboxEvent saved = outboxEventRepository.saveAndFlush(event);

        try {
            assertTrue(deliveryService.registerFailure(
                    saved.getEventId(),
                    token,
                    0,
                    "SQS unavailable"
            ));

            OutboxEvent persisted = outboxEventRepository
                    .findById(saved.getEventId())
                    .orElseThrow();

            assertEquals(1, persisted.getAttempts());
            assertEquals("SQS unavailable", persisted.getLastError());
            assertTrue(persisted.getNextAttemptAt()
                    .isAfter(Instant.now()));
            assertNull(persisted.getClaimToken());
            assertNull(persisted.getClaimedUntil());
            assertNull(persisted.getPublishedAt());
        } finally {
            outboxEventRepository.deleteById(saved.getEventId());
        }
    }
}