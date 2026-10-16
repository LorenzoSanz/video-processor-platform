package com.lorenzosanz.uploadservice.repository;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldPersistAndRetrievePendingEvent() {
        UUID uploadId = UUID.randomUUID();

        String payload =
                "{\"uploadId\":\"" + uploadId + "\"}";

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                payload);

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        assertNotNull(saved.getEventId());

        OutboxEvent retrieved = outboxEventRepository
                .findById(saved.getEventId())
                .orElseThrow();

        assertEquals("UPLOAD", retrieved.getAggregateType());
        assertEquals(uploadId, retrieved.getAggregateId());
        assertEquals(
                "VIDEO_PROCESSING_REQUESTED",
                retrieved.getEventType());
        assertEquals(payload, retrieved.getPayload());

        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getNextAttemptAt());
        assertEquals(0, retrieved.getAttempts());
        assertNull(retrieved.getPublishedAt());
        assertNull(retrieved.getLastError());

        assertTrue(outboxEventRepository.existsById(
                saved.getEventId()));
    }

    @Test
    void shouldPersistClaimLeaseAndClearItWhenPublished() {
        UUID uploadId = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();

        Instant now = Instant.now();
        Instant claimedUntil = now.plusSeconds(60);
        Instant publishedAt = now.plusSeconds(10);

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"uploadId\":\"" + uploadId + "\"}"
        );

        event.claim(claimToken, claimedUntil);

        OutboxEvent saved = outboxEventRepository.saveAndFlush(event);

        OutboxEvent retrieved = outboxEventRepository
                .findById(saved.getEventId())
                .orElseThrow();

        assertEquals(claimToken, retrieved.getClaimToken());
        assertEquals(claimedUntil, retrieved.getClaimedUntil());

        retrieved.markPublished(publishedAt);
        outboxEventRepository.saveAndFlush(retrieved);

        OutboxEvent published = outboxEventRepository
                .findById(saved.getEventId())
                .orElseThrow();

        assertEquals(publishedAt, published.getPublishedAt());
        assertNull(published.getClaimToken());
        assertNull(published.getClaimedUntil());
    }
}