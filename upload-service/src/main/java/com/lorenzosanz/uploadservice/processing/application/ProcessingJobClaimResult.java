package com.lorenzosanz.uploadservice.processing.application;

import java.time.Instant;
import java.util.Objects;

import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;

public sealed interface ProcessingJobClaimResult
        permits ProcessingJobClaimResult.Claimed,
                ProcessingJobClaimResult.NotFound,
                ProcessingJobClaimResult.LeaseActive,
                ProcessingJobClaimResult.RetryNotDue,
                ProcessingJobClaimResult.AlreadyCompleted,
                ProcessingJobClaimResult.TerminallyFailed {

    record Claimed(ProcessingJob job)
            implements ProcessingJobClaimResult {

        public Claimed {
            Objects.requireNonNull(job, "Job is required");
        }
    }

    record NotFound()
            implements ProcessingJobClaimResult {
    }

    record LeaseActive(Instant leaseUntil)
            implements ProcessingJobClaimResult {

        public LeaseActive {
            Objects.requireNonNull(leaseUntil, "Lease expiration is required");
        }
    }

    record RetryNotDue(Instant nextAttemptAt)
            implements ProcessingJobClaimResult {

        public RetryNotDue {
            Objects.requireNonNull(
                    nextAttemptAt,
                    "Next attempt time is required"
            );
        }
    }

    record AlreadyCompleted()
            implements ProcessingJobClaimResult {
    }

    record TerminallyFailed()
            implements ProcessingJobClaimResult {
    }
}