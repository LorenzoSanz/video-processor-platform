CREATE TABLE outbox_event (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,

    CONSTRAINT chk_outbox_event_attempts_nonnegative
        CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_event_pending
    ON outbox_event (next_attempt_at, created_at)
    WHERE published_at IS NULL;