package com.lorenzosanz.uploadservice.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @NotNull
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @NotBlank
    @Size(max = 150)
    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @NotBlank
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Min(0)
    @Column(name = "attempts", nullable = false)
    private int attempts;

    @NotNull
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public OutboxEvent() {
    }

    public OutboxEvent(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload) {

        Instant now = Instant.now();

        this.eventId = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = now;
        this.publishedAt = null;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.lastError = null;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void markPublished(Instant publishedAt) {
        if (this.publishedAt != null) {
            throw new IllegalStateException("Event has already been published");
        }

        this.publishedAt = publishedAt;
    }

    public void registerPublicationFailure(
            String errorMessage,
            Instant retryAt) {

        if (this.publishedAt != null) {
            throw new IllegalStateException(
                    "Cannot retry an already published event");
        }

        this.attempts++;
        this.lastError = errorMessage;
        this.nextAttemptAt = retryAt;
    }
}
