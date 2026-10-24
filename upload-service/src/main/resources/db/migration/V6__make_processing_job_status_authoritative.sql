-- The processing job is the source of truth for processing status.
-- Preserve legacy upload statuses before dropping upload.status.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM upload
        WHERE status NOT IN (
            'PENDING',
            'PROCESSING',
            'COMPLETED',
            'FAILED'
        )
    ) THEN
        RAISE EXCEPTION
            'Cannot migrate: upload contains an unsupported status';
    END IF;

    -- Existing jobs should already be synchronized with their uploads.
    -- Stop rather than silently discard evidence of inconsistent state.
    IF EXISTS (
        SELECT 1
        FROM upload u
        JOIN processing_job j ON j.upload_id = u.upload_id
        WHERE u.status <> j.status
    ) THEN
        RAISE EXCEPTION
            'Cannot migrate: upload and processing_job statuses disagree';
    END IF;
END $$;

-- V5 only backfilled PENDING uploads.
-- Add a job for every remaining legacy upload.
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
    u.upload_id,
    CASE u.status
        WHEN 'COMPLETED' THEN 'COMPLETED'
        WHEN 'FAILED' THEN 'FAILED'
        ELSE 'PENDING'
    END,
    0,
    u.created_at,
    u.created_at
FROM upload u
WHERE NOT EXISTS (
    SELECT 1
    FROM processing_job j
    WHERE j.upload_id = u.upload_id
);

-- Verify that every upload now has a processing job.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM upload u
        LEFT JOIN processing_job j ON j.upload_id = u.upload_id
        WHERE j.job_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cannot migrate: one or more uploads have no processing job';
    END IF;
END $$;

-- Drop the duplicated persisted status only after the backfill succeeds.
ALTER TABLE upload
    DROP COLUMN status;
