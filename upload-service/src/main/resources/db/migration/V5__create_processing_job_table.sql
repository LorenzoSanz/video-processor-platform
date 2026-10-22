CREATE TABLE processing_job (
    job_id UUID PRIMARY KEY,
    upload_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claim_token UUID,
    lease_until TIMESTAMP WITH TIME ZONE,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(2000),
    output_path VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_processing_job_upload
        FOREIGN KEY (upload_id)
        REFERENCES upload(upload_id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_processing_job_upload
        UNIQUE (upload_id),

    CONSTRAINT chk_processing_job_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk_processing_job_attempts
        CHECK (attempts >= 0),

    CONSTRAINT chk_processing_job_lease
        CHECK (
            (
                status = 'PROCESSING'
                AND claim_token IS NOT NULL
                AND lease_until IS NOT NULL
            )
            OR
            (
                status <> 'PROCESSING'
                AND claim_token IS NULL
                AND lease_until IS NULL
            )
        )
);

CREATE INDEX idx_processing_job_pending
    ON processing_job (next_attempt_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_processing_job_expired_lease
    ON processing_job (lease_until)
    WHERE status = 'PROCESSING';

-- Backfill for uploads created before ProcessingJob existed.
-- Their Outbox messages retain the existing uploadId payload.
INSERT INTO processing_job (
    job_id,
    upload_id,
    status,
    attempts,
    next_attempt_at,
    created_at
)
SELECT
    gen_random_uuid(),
    upload_id,
    'PENDING',
    0,
    created_at,
    created_at
FROM upload
WHERE status = 'PENDING';