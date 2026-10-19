package com.lorenzosanz.uploadservice.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.messaging.OutboxMessage;
import com.lorenzosanz.uploadservice.messaging.OutboxMessageSender;

@Service
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "enabled",
        havingValue = "true"
)
public class OutboxPublisherService {

    private static final Logger logger =
            LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxClaimService outboxClaimService;
    private final OutboxDeliveryService outboxDeliveryService;
    private final OutboxMessageSender outboxMessageSender;

    public OutboxPublisherService(
            OutboxClaimService outboxClaimService,
            OutboxDeliveryService outboxDeliveryService,
            OutboxMessageSender outboxMessageSender) {
        this.outboxClaimService = outboxClaimService;
        this.outboxDeliveryService = outboxDeliveryService;
        this.outboxMessageSender = outboxMessageSender;
    }

    public int publishPending(int batchSize, Duration leaseDuration) {
        List<OutboxEvent> events =
                outboxClaimService.claimPendingEvents(
                        batchSize,
                        leaseDuration
                );

        int publishedCount = 0;

        for (OutboxEvent event : events) {
            UUID eventId = event.getEventId();
            UUID claimToken = event.getClaimToken();

            if (claimToken == null) {
                logger.error(
                        "Claimed event {} has no claim token",
                        eventId
                );
                continue;
            }

            OutboxMessage message = new OutboxMessage(
                    eventId,
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getPayload()
            );

            try {
                outboxMessageSender.send(message);
            } catch (RuntimeException publicationException) {
                boolean retryScheduled =
                        outboxDeliveryService.registerFailure(
                                eventId,
                                claimToken,
                                event.getAttempts(),
                                publicationException.getMessage()
                        );

                if (!retryScheduled) {
                    logger.warn(
                            "Could not schedule retry for event {}: "
                                    + "the claim is no longer current",
                            eventId
                    );
                }

                continue;
            }

            boolean markedPublished =
                    outboxDeliveryService.markPublished(
                            eventId,
                            claimToken
                    );

            if (markedPublished) {
                publishedCount++;
            } else {
                logger.warn(
                        "Event {} was sent, but its claim was no longer "
                                + "current when marking it published",
                        eventId
                );
            }
        }

        return publishedCount;
    }
}