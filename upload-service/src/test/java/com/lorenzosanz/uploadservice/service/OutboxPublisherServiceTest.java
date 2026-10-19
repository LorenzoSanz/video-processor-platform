package com.lorenzosanz.uploadservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;
import com.lorenzosanz.uploadservice.messaging.OutboxMessage;
import com.lorenzosanz.uploadservice.messaging.OutboxMessageSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {

    @Mock
    private OutboxClaimService outboxClaimService;

    @Mock
    private OutboxDeliveryService outboxDeliveryService;

    @Mock
    private OutboxMessageSender outboxMessageSender;

    @InjectMocks
    private OutboxPublisherService outboxPublisherService;

    @Test
    void shouldSendMessageAndMarkEventPublished() {
        UUID uploadId = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();

        OutboxEvent event = createClaimedEvent(uploadId, claimToken);

        when(outboxClaimService.claimPendingEvents(
                10, Duration.ofSeconds(30)))
                .thenReturn(List.of(event));

        when(outboxDeliveryService.markPublished(
                event.getEventId(), claimToken))
                .thenReturn(true);

        int publishedCount = outboxPublisherService.publishPending(
                10, Duration.ofSeconds(30));

        assertEquals(1, publishedCount);

        ArgumentCaptor<OutboxMessage> captor =
                ArgumentCaptor.forClass(OutboxMessage.class);

        verify(outboxMessageSender).send(captor.capture());

        OutboxMessage message = captor.getValue();

        assertEquals(event.getEventId(), message.eventId());
        assertEquals("UPLOAD", message.aggregateType());
        assertEquals(uploadId, message.aggregateId());
        assertEquals(
                "VIDEO_PROCESSING_REQUESTED",
                message.eventType()
        );

        verify(outboxDeliveryService).markPublished(
                event.getEventId(), claimToken);

        verify(outboxDeliveryService, never()).registerFailure(
                any(), any(), any(Integer.class), any());
    }

    @Test
    void shouldScheduleRetryWhenSendingFails() {
        UUID uploadId = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();

        OutboxEvent event = createClaimedEvent(uploadId, claimToken);

        when(outboxClaimService.claimPendingEvents(
                10, Duration.ofSeconds(30)))
                .thenReturn(List.of(event));

        doThrow(new IllegalStateException("SQS unavailable"))
                .when(outboxMessageSender)
                .send(any(OutboxMessage.class));

        when(outboxDeliveryService.registerFailure(
                event.getEventId(),
                claimToken,
                event.getAttempts(),
                "SQS unavailable"))
                .thenReturn(true);

        int publishedCount = outboxPublisherService.publishPending(
                10, Duration.ofSeconds(30));

        assertEquals(0, publishedCount);

        verify(outboxDeliveryService).registerFailure(
                event.getEventId(),
                claimToken,
                event.getAttempts(),
                "SQS unavailable"
        );

        verify(outboxDeliveryService, never()).markPublished(
                any(), any());
    }

    @Test
    void shouldNotCountEventAsPublishedWhenClaimIsStale() {
        UUID uploadId = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();

        OutboxEvent event = createClaimedEvent(uploadId, claimToken);

        when(outboxClaimService.claimPendingEvents(
                10, Duration.ofSeconds(30)))
                .thenReturn(List.of(event));

        when(outboxDeliveryService.markPublished(
                event.getEventId(), claimToken))
                .thenReturn(false);

        int publishedCount = outboxPublisherService.publishPending(
                10, Duration.ofSeconds(30));

        assertEquals(0, publishedCount);

        verify(outboxMessageSender).send(any(OutboxMessage.class));

        verify(outboxDeliveryService).markPublished(
                event.getEventId(), claimToken);

        verify(outboxDeliveryService, never()).registerFailure(
                any(), any(), any(Integer.class), any());
    }

    private OutboxEvent createClaimedEvent(
            UUID uploadId,
            UUID claimToken) {

        OutboxEvent event = new OutboxEvent(
                "UPLOAD",
                uploadId,
                "VIDEO_PROCESSING_REQUESTED",
                "{\"uploadId\":\"" + uploadId + "\"}"
        );

        event.claim(
                claimToken,
                Instant.now().plusSeconds(120)
        );

        return event;
    }
}