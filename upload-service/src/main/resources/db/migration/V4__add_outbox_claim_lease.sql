ALTER TABLE outbox_event
    ADD COLUMN claim_token UUID,
    ADD COLUMN claimed_until TIMESTAMP WITH TIME ZONE;

ALTER TABLE outbox_event
    ADD CONSTRAINT chk_outbox_event_claim_fields
    CHECK (
        (
            claim_token IS NULL
            AND claimed_until IS NULL
        )
        OR
        (
            claim_token IS NOT NULL
            AND claimed_until IS NOT NULL
            AND published_at IS NULL
        )
    );