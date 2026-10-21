package com.lorenzosanz.uploadservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.lorenzosanz.uploadservice.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.repository.OutboxEventRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class OutboxClaimServiceIntegrationTest {

    @Autowired
    private OutboxClaimService outboxClaimService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldClaimEventAndRespectActiveLease() {
        UUID uploadId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"uploadId\":\"" + uploadId + "\"}"
        );

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        UUID eventId = saved.getEventId();

        try {
            List<OutboxEvent> firstClaim =
                    outboxClaimService.claimPendingEvents(
                            1000,
                            Duration.ofMinutes(1));

            OutboxEvent claimed = firstClaim.stream()
                    .filter(candidate ->
                            eventId.equals(candidate.getEventId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Expected event to be claimed"));

            assertNotNull(claimed.getClaimToken());
            assertNotNull(claimed.getClaimedUntil());
            assertTrue(
                    claimed.getClaimedUntil().isAfter(Instant.now()));

            OutboxEvent persisted =
                    outboxEventRepository.findById(eventId)
                            .orElseThrow();

            assertEquals(
                    claimed.getClaimToken(),
                    persisted.getClaimToken());

           Duration precisionDifference = Duration.between(
        claimed.getClaimedUntil(),
        persisted.getClaimedUntil()
).abs();

assertTrue(
        precisionDifference.compareTo(Duration.ofNanos(1_000)) <= 0,
        "The persisted lease must match within PostgreSQL timestamp precision"
);

            List<OutboxEvent> secondClaim =
                    outboxClaimService.claimPendingEvents(
                            1000,
                            Duration.ofMinutes(1));

            assertFalse(secondClaim.stream()
                    .anyMatch(candidate ->
                            eventId.equals(candidate.getEventId())));

        } finally {
            outboxEventRepository.deleteById(eventId);
        }
    }
}