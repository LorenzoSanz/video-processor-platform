package com.lorenzosanz.uploadservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.repository.OutboxEventRepository;

@Service
public class OutboxClaimService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxClaimService(
            OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public List<OutboxEvent> claimPendingEvents(
            int batchSize,
            Duration leaseDuration) {

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Batch size must be positive");
        }

        if (leaseDuration == null
                || leaseDuration.isZero()
                || leaseDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "Lease duration must be positive");
        }

        Instant now = Instant.now();

        List<OutboxEvent> events =
                outboxEventRepository.findClaimableEvents(
                        now,
                        batchSize);

        if (events.isEmpty()) {
            return List.of();
        }

        Instant claimedUntil = now.plus(leaseDuration);

        for (OutboxEvent event : events) {
            event.claim(UUID.randomUUID(), claimedUntil);
        }

        outboxEventRepository.flush();

        return List.copyOf(events);
    }
}