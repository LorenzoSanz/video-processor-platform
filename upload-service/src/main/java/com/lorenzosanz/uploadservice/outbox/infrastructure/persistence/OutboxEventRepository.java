package com.lorenzosanz.uploadservice.outbox.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lorenzosanz.uploadservice.outbox.domain.OutboxEvent;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {
                @Query(value = """
        SELECT *
        FROM outbox_event
        WHERE published_at IS NULL
          AND next_attempt_at <= :now
          AND (
              claimed_until IS NULL
              OR claimed_until <= :now
          )
        ORDER BY next_attempt_at ASC, created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
List<OutboxEvent> findClaimableEvents(
        @Param("now") Instant now,
        @Param("batchSize") int batchSize);

@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query(value = """
        UPDATE outbox_event
        SET published_at = :publishedAt,
            claim_token = NULL,
            claimed_until = NULL
        WHERE event_id = :eventId
          AND claim_token = :claimToken
          AND claimed_until > :now
          AND published_at IS NULL
        """, nativeQuery = true)
int markPublishedIfCurrentClaim(
        @Param("eventId") UUID eventId,
        @Param("claimToken") UUID claimToken,
        @Param("now") Instant now,
        @Param("publishedAt") Instant publishedAt);


@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query(value = """
        UPDATE outbox_event
        SET attempts = attempts + 1,
            last_error = :lastError,
            next_attempt_at = :nextAttemptAt,
            claim_token = NULL,
            claimed_until = NULL
        WHERE event_id = :eventId
          AND claim_token = :claimToken
          AND claimed_until > :now
          AND published_at IS NULL
        """, nativeQuery = true)
int scheduleRetryIfCurrentClaim(
        @Param("eventId") UUID eventId,
        @Param("claimToken") UUID claimToken,
        @Param("now") Instant now,
        @Param("nextAttemptAt") Instant nextAttemptAt,
        @Param("lastError") String lastError);
}