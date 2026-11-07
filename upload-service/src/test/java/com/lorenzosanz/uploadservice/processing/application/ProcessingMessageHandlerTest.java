
package com.lorenzosanz.uploadservice.processing.application;

import com.lorenzosanz.uploadservice.processing.application.port.ProcessingCancellationToken;
import com.lorenzosanz.uploadservice.processing.application.port.ProcessingCommand;
import com.lorenzosanz.uploadservice.processing.application.port.ProcessingJobProcessor;
import com.lorenzosanz.uploadservice.processing.application.port.ProcessingResult;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.processing.application.ProcessingMessageHandlingResult.AcknowledgementReason;
import com.lorenzosanz.uploadservice.processing.application.ProcessingMessageHandlingResult.UnacknowledgedReason;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingMessageHandlerTest {

    private static final UUID JOB_ID =
            UUID.randomUUID();

    private static final UUID UPLOAD_ID =
            UUID.randomUUID();

    private static final UUID CLAIM_TOKEN =
            UUID.randomUUID();

    private static final Duration LEASE_DURATION =
            Duration.ofMinutes(2);

    private static final Instant LEASE_UNTIL =
            Instant.parse("2026-11-07T12:00:00Z");

    private static final Instant NEXT_ATTEMPT_AT =
            Instant.parse("2026-11-07T12:05:00Z");

    private static final String OUTPUT_PATH =
            "processed/" + UPLOAD_ID + ".mp4";

    @Mock
    private ProcessingJobClaimService claimService;

    @Mock
    private ProcessingJobCompletionService completionService;

    @Mock
    private ProcessingJobFailureService failureService;

    @Mock
    private ProcessingJobProcessor processor;

    private ProcessingMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProcessingMessageHandler(
                claimService,
                completionService,
                failureService,
                processor
        );
    }

    @Test
    void shouldProcessAndAcknowledgeSuccessfully() {
        ProcessingJob job = claimedJob();
        ProcessingCommand command = expectedCommand();

        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.Claimed(job)
                );

        when(processor.process(
                eq(command),
                any(ProcessingCancellationToken.class)
        )).thenReturn(
                new ProcessingResult.Succeeded(OUTPUT_PATH)
        );

        when(completionService.complete(
                JOB_ID,
                CLAIM_TOKEN,
                OUTPUT_PATH
        )).thenReturn(true);

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.Acknowledged(
                        AcknowledgementReason.COMPLETED
                ),
                result
        );

        verify(claimService).claim(UPLOAD_ID, LEASE_DURATION);

        ArgumentCaptor<ProcessingCancellationToken> tokenCaptor =
                ArgumentCaptor.forClass(
                        ProcessingCancellationToken.class
                );

        verify(processor).process(
                eq(command),
                tokenCaptor.capture()
        );

        assertFalse(
                tokenCaptor.getValue().isCancellationRequested()
        );

        verify(completionService).complete(
                JOB_ID,
                CLAIM_TOKEN,
                OUTPUT_PATH
        );

        verifyNoInteractions(failureService);
    }

    @Test
    void shouldLeaveMessageUnacknowledgedWhenCompletionIsRejected() {
        ProcessingJob job = claimedJob();

        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.Claimed(job)
                );

        when(processor.process(
                eq(expectedCommand()),
                any(ProcessingCancellationToken.class)
        )).thenReturn(
                new ProcessingResult.Succeeded(OUTPUT_PATH)
        );

        when(completionService.complete(
                JOB_ID,
                CLAIM_TOKEN,
                OUTPUT_PATH
        )).thenReturn(false);

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.LeaveUnacknowledged(
                        UnacknowledgedReason.OWNERSHIP_LOST
                ),
                result
        );

        verifyNoInteractions(failureService);
    }

    @Test
    void shouldDeferMessageWhenProcessingFailureIsRetryable() {
        ProcessingJob job = claimedJob();

        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.Claimed(job)
                );

        when(processor.process(
                eq(expectedCommand()),
                any(ProcessingCancellationToken.class)
        )).thenReturn(
                new ProcessingResult.Failed(
                        "Temporary processing error",
                        ProcessingResult.FailureKind.RETRYABLE
                )
        );

        when(failureService.fail(
                JOB_ID,
                CLAIM_TOKEN,
                "Temporary processing error",
                true
        )).thenReturn(
                new ProcessingJobFailureResult.RetryScheduled(
                        NEXT_ATTEMPT_AT
                )
        );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.DeferUntil(
                        NEXT_ATTEMPT_AT
                ),
                result
        );

        verify(failureService).fail(
                JOB_ID,
                CLAIM_TOKEN,
                "Temporary processing error",
                true
        );

        verifyNoInteractions(completionService);
    }

    @Test
    void shouldAcknowledgeMessageWhenFailureIsPermanent() {
        ProcessingJob job = claimedJob();

        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.Claimed(job)
                );

        when(processor.process(
                eq(expectedCommand()),
                any(ProcessingCancellationToken.class)
        )).thenReturn(
                new ProcessingResult.Failed(
                        "Unsupported video format",
                        ProcessingResult.FailureKind.PERMANENT
                )
        );

        when(failureService.fail(
                JOB_ID,
                CLAIM_TOKEN,
                "Unsupported video format",
                false
        )).thenReturn(
                new ProcessingJobFailureResult.TerminalFailure(
                        ProcessingJobFailureResult.TerminalFailureReason
                                .NON_RETRYABLE_FAILURE
                )
        );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.Acknowledged(
                        AcknowledgementReason.TERMINAL_FAILURE
                ),
                result
        );

        verify(failureService).fail(
                JOB_ID,
                CLAIM_TOKEN,
                "Unsupported video format",
                false
        );

        verifyNoInteractions(completionService);
    }

    @Test
    void shouldAcknowledgeMessageWhenMaximumAttemptsAreReached() {
        ProcessingJob job = claimedJob();

        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.Claimed(job)
                );

        when(processor.process(
                eq(expectedCommand()),
                any(ProcessingCancellationToken.class)
        )).thenReturn(
                new ProcessingResult.Failed(
                        "Repeated processing failure",
                        ProcessingResult.FailureKind.RETRYABLE
                )
        );

        when(failureService.fail(
                JOB_ID,
                CLAIM_TOKEN,
                "Repeated processing failure",
                true
        )).thenReturn(
                new ProcessingJobFailureResult.TerminalFailure(
                        ProcessingJobFailureResult.TerminalFailureReason
                                .MAX_ATTEMPTS_REACHED
                )
        );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.Acknowledged(
                        AcknowledgementReason.TERMINAL_FAILURE
                ),
                result
        );
    }

    @Test
    void shouldLeaveMessageUnacknowledgedWhenFailureIsRejected() {
        ProcessingJob job = claimedJob();

        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.Claimed(job)
                );

        when(processor.process(
                eq(expectedCommand()),
                any(ProcessingCancellationToken.class)
        )).thenReturn(
                new ProcessingResult.Failed(
                        "Temporary error",
                        ProcessingResult.FailureKind.RETRYABLE
                )
        );

        when(failureService.fail(
                JOB_ID,
                CLAIM_TOKEN,
                "Temporary error",
                true
        )).thenReturn(
                new ProcessingJobFailureResult.Rejected()
        );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.LeaveUnacknowledged(
                        UnacknowledgedReason.OWNERSHIP_LOST
                ),
                result
        );
    }

    @Test
    void shouldLeaveMessageUnacknowledgedWhenFailureJobIsNotFound() {
        ProcessingJob job = claimedJob();

        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.Claimed(job)
                );

        when(processor.process(
                eq(expectedCommand()),
                any(ProcessingCancellationToken.class)
        )).thenReturn(
                new ProcessingResult.Failed(
                        "Temporary error",
                        ProcessingResult.FailureKind.RETRYABLE
                )
        );

        when(failureService.fail(
                JOB_ID,
                CLAIM_TOKEN,
                "Temporary error",
                true
        )).thenReturn(
                new ProcessingJobFailureResult.NotFound()
        );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.LeaveUnacknowledged(
                        UnacknowledgedReason.JOB_NOT_FOUND
                ),
                result
        );
    }

    @Test
    void shouldDeferMessageWhenAnotherWorkerHasActiveLease() {
        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.LeaseActive(
                                LEASE_UNTIL
                        )
                );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.DeferUntil(
                        LEASE_UNTIL
                ),
                result
        );

        verifyNoInteractions(
                processor,
                completionService,
                failureService
        );
    }

    @Test
    void shouldDeferMessageWhenRetryIsNotDue() {
        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.RetryNotDue(
                                NEXT_ATTEMPT_AT
                        )
                );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.DeferUntil(
                        NEXT_ATTEMPT_AT
                ),
                result
        );

        verifyNoInteractions(
                processor,
                completionService,
                failureService
        );
    }

    @Test
    void shouldAcknowledgeAlreadyCompletedJobWithoutProcessingAgain() {
        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.AlreadyCompleted()
                );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.Acknowledged(
                        AcknowledgementReason.ALREADY_COMPLETED
                ),
                result
        );

        verifyNoInteractions(
                processor,
                completionService,
                failureService
        );
    }

    @Test
    void shouldAcknowledgeTerminallyFailedJobWithoutProcessingAgain() {
        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.TerminallyFailed()
                );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.Acknowledged(
                        AcknowledgementReason.TERMINAL_FAILURE
                ),
                result
        );

        verifyNoInteractions(
                processor,
                completionService,
                failureService
        );
    }

    @Test
    void shouldLeaveMessageUnacknowledgedWhenJobDoesNotExist() {
        when(claimService.claim(UPLOAD_ID, LEASE_DURATION))
                .thenReturn(
                        new ProcessingJobClaimResult.NotFound()
                );

        ProcessingMessageHandlingResult result = handler.handle(
                UPLOAD_ID,
                LEASE_DURATION
        );

        assertEquals(
                new ProcessingMessageHandlingResult.LeaveUnacknowledged(
                        UnacknowledgedReason.JOB_NOT_FOUND
                ),
                result
        );

        verifyNoInteractions(
                processor,
                completionService,
                failureService
        );
    }

    private ProcessingCommand expectedCommand() {
        return new ProcessingCommand(
                JOB_ID,
                UPLOAD_ID,
                CLAIM_TOKEN
        );
    }

    private ProcessingJob claimedJob() {
        Instant claimedAt = Instant.now();

        ProcessingJob job = new ProcessingJob(
                JOB_ID,
                UPLOAD_ID,
                claimedAt.minusSeconds(1)
        );

        job.claim(
                CLAIM_TOKEN,
                claimedAt,
                claimedAt.plus(LEASE_DURATION)
        );

        return job;
    }
}
