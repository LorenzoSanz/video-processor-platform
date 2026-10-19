package com.lorenzosanz.uploadservice.messaging;

import java.util.UUID;

public record OutboxMessage(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload
) {
}