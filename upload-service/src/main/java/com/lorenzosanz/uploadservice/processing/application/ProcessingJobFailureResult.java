
package com.lorenzosanz.uploadservice.processing.application;

import java.time.Instant;
import java.util.Objects;

public sealed interface ProcessingJobFailureResult
        permits ProcessingJobFailureResult.RetryScheduled,
                ProcessingJobFailureResult.TerminalFailure,
                ProcessingJobFailureResult.Rejected,
                ProcessingJobFailureResult.NotFound {

    record RetryScheduled(Instant nextAttemptAt)
            implements ProcessingJobFailureResult {

        public RetryScheduled {
            Objects.requireNonNull(
                    nextAttemptAt,
                    "Next attempt time is required"
            );
        }
    }

    record TerminalFailure(TerminalFailureReason reason)
            implements ProcessingJobFailureResult {

        public TerminalFailure {
            Objects.requireNonNull(
                    reason,
                    "Terminal failure reason is required"
            );
        }
    }

    record Rejected()
            implements ProcessingJobFailureResult {
    }

    record NotFound()
            implements ProcessingJobFailureResult {
    }

    enum TerminalFailureReason {
        NON_RETRYABLE_FAILURE,
        MAX_ATTEMPTS_REACHED
    }
}
