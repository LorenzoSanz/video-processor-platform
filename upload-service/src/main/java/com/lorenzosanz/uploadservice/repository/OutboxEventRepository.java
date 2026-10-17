package com.lorenzosanz.uploadservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;

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
}