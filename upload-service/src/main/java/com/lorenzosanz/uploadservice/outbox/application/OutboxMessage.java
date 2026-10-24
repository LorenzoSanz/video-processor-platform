package com.lorenzosanz.uploadservice.outbox.application;

import java.util.UUID;

public record OutboxMessage(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload
) {
}