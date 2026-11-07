
package com.lorenzosanz.uploadservice.processing.application;

import java.time.Duration;
import java.util.UUID;

import com.lorenzosanz.uploadservice.processing.application.port.ProcessingCancellationToken;
import com.lorenzosanz.uploadservice.processing.application.port.ProcessingCommand;
import com.lorenzosanz.uploadservice.processing.application.port.ProcessingJobProcessor;
import com.lorenzosanz.uploadservice.processing.application.port.ProcessingResult;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;

public class ProcessingMessageHandler {

    private final ProcessingJobClaimService claimService;
    private final ProcessingJobCompletionService completionService;
    private final ProcessingJobFailureService failureService;
    private final ProcessingJobProcessor processor;

    public ProcessingMessageHandler(
            ProcessingJobClaimService claimService,
            ProcessingJobCompletionService completionService,
            ProcessingJobFailureService failureService,
            ProcessingJobProcessor processor) {

        this.claimService = claimService;
        this.completionService = completionService;
        this.failureService = failureService;
        this.processor = processor;
    }

    public ProcessingMessageHandlingResult handle(
            UUID uploadId,
            Duration leaseDuration) {

        ProcessingJobClaimResult claimResult = claimService.claim(
                uploadId,
                leaseDuration
        );

        if (claimResult instanceof ProcessingJobClaimResult.Claimed claimed) {
            return processClaimedJob(claimed.job());
        }

        if (claimResult instanceof ProcessingJobClaimResult.LeaseActive leaseActive) {
            return new ProcessingMessageHandlingResult.DeferUntil(
                    leaseActive.leaseUntil()
            );
        }

        if (claimResult instanceof ProcessingJobClaimResult.RetryNotDue retryNotDue) {
            return new ProcessingMessageHandlingResult.DeferUntil(
                    retryNotDue.nextAttemptAt()
            );
        }

        if (claimResult instanceof ProcessingJobClaimResult.AlreadyCompleted) {
            return acknowledged(
                    ProcessingMessageHandlingResult.AcknowledgementReason.ALREADY_COMPLETED
            );
        }

        if (claimResult instanceof ProcessingJobClaimResult.TerminallyFailed) {
            return acknowledged(
                    ProcessingMessageHandlingResult.AcknowledgementReason.TERMINAL_FAILURE
            );
        }

        if (claimResult instanceof ProcessingJobClaimResult.NotFound) {
            return unacknowledged(
                    ProcessingMessageHandlingResult.UnacknowledgedReason.JOB_NOT_FOUND
            );
        }

        throw new IllegalStateException(
                "Unsupported processing claim result: " + claimResult
        );
    }

    private ProcessingMessageHandlingResult processClaimedJob(
            ProcessingJob job) {

        UUID jobId = job.getJobId();
        UUID uploadId = job.getUploadId();
        UUID claimToken = job.getClaimToken();

        ProcessingCommand command = new ProcessingCommand(
                jobId,
                uploadId,
                claimToken
        );

        ProcessingCancellationToken cancellationToken =
                new ProcessingCancellationToken();

        ProcessingResult processingResult = processor.process(
                command,
                cancellationToken
        );

        if (processingResult instanceof ProcessingResult.Succeeded succeeded) {

            boolean completed = completionService.complete(
                    jobId,
                    claimToken,
                    succeeded.outputPath()
            );

            if (!completed) {
                return unacknowledged(
                        ProcessingMessageHandlingResult.UnacknowledgedReason.OWNERSHIP_LOST
                );
            }

            return acknowledged(
                    ProcessingMessageHandlingResult.AcknowledgementReason.COMPLETED
            );
        }

        if (processingResult instanceof ProcessingResult.Failed failed) {

            boolean retryable =
                    failed.failureKind() == ProcessingResult.FailureKind.RETRYABLE;

            ProcessingJobFailureResult failureResult = failureService.fail(
                    jobId,
                    claimToken,
                    failed.errorMessage(),
                    retryable
            );

            return handleFailureResult(failureResult);
        }

        throw new IllegalStateException(
                "Unsupported processing result: " + processingResult
        );
    }

    private ProcessingMessageHandlingResult handleFailureResult(
            ProcessingJobFailureResult failureResult) {

        if (failureResult instanceof ProcessingJobFailureResult.RetryScheduled retryScheduled) {
            return new ProcessingMessageHandlingResult.DeferUntil(
                    retryScheduled.nextAttemptAt()
            );
        }

        if (failureResult instanceof ProcessingJobFailureResult.TerminalFailure) {
            return acknowledged(
                    ProcessingMessageHandlingResult.AcknowledgementReason.TERMINAL_FAILURE
            );
        }

        if (failureResult instanceof ProcessingJobFailureResult.Rejected) {
            return unacknowledged(
                    ProcessingMessageHandlingResult.UnacknowledgedReason.OWNERSHIP_LOST
            );
        }

        if (failureResult instanceof ProcessingJobFailureResult.NotFound) {
            return unacknowledged(
                    ProcessingMessageHandlingResult.UnacknowledgedReason.JOB_NOT_FOUND
            );
        }

        throw new IllegalStateException(
                "Unsupported processing failure result: " + failureResult
        );
    }

    private ProcessingMessageHandlingResult acknowledged(
            ProcessingMessageHandlingResult.AcknowledgementReason reason) {

        return new ProcessingMessageHandlingResult.Acknowledged(reason);
    }

    private ProcessingMessageHandlingResult unacknowledged(
            ProcessingMessageHandlingResult.UnacknowledgedReason reason) {

        return new ProcessingMessageHandlingResult.LeaveUnacknowledged(reason);
    }
}