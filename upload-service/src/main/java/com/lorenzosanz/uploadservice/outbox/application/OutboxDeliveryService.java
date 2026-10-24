package com.lorenzosanz.uploadservice.outbox.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.outbox.infrastructure.persistence.OutboxEventRepository;

@Service
public class OutboxDeliveryService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxDeliveryService(
            OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public boolean markPublished(UUID eventId, UUID claimToken) {
        Instant now = Instant.now();

        return outboxEventRepository.markPublishedIfCurrentClaim(
                eventId,
                claimToken,
                now,
                now
        ) == 1;
    }

    @Transactional
    public boolean registerFailure(
            UUID eventId,
            UUID claimToken,
            int previousAttempts,
            String errorMessage) {

        if (previousAttempts < 0) {
            throw new IllegalArgumentException(
                    "Previous attempts cannot be negative");
        }

        Instant now = Instant.now();

        int exponent = Math.min(previousAttempts, 10);
        long delaySeconds = Math.min(
                5L * (1L << exponent),
                3600L
        );

        Instant nextAttemptAt = now.plusSeconds(delaySeconds);

        String safeError = errorMessage == null
                || errorMessage.isBlank()
                ? "Unknown publication failure"
                : errorMessage;

        if (safeError.length() > 2000) {
            safeError = safeError.substring(0, 2000);
        }

        return outboxEventRepository.scheduleRetryIfCurrentClaim(
                eventId,
                claimToken,
                now,
                nextAttemptAt,
                safeError
        ) == 1;
    }
}