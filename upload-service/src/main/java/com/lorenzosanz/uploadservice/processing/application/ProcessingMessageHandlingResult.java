
package com.lorenzosanz.uploadservice.processing.application;

import java.time.Instant;
import java.util.Objects;

public sealed interface ProcessingMessageHandlingResult
        permits ProcessingMessageHandlingResult.Acknowledged,
                ProcessingMessageHandlingResult.DeferUntil,
                ProcessingMessageHandlingResult.LeaveUnacknowledged {

    record Acknowledged(AcknowledgementReason reason)
            implements ProcessingMessageHandlingResult {

        public Acknowledged {
            Objects.requireNonNull(
                    reason,
                    "Acknowledgement reason is required"
            );
        }
    }

    record DeferUntil(Instant eligibleAt)
            implements ProcessingMessageHandlingResult {

        public DeferUntil {
            Objects.requireNonNull(
                    eligibleAt,
                    "Eligibility time is required"
            );
        }
    }

    record LeaveUnacknowledged(UnacknowledgedReason reason)
            implements ProcessingMessageHandlingResult {

        public LeaveUnacknowledged {
            Objects.requireNonNull(
                    reason,
                    "Unacknowledged reason is required"
            );
        }
    }

    enum AcknowledgementReason {
        COMPLETED,
        ALREADY_COMPLETED,
        TERMINAL_FAILURE
    }

    enum UnacknowledgedReason {
        JOB_NOT_FOUND,
        CLAIM_REJECTED,
        OWNERSHIP_LOST
    }
}
