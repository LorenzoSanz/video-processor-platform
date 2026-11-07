
package com.lorenzosanz.uploadservice.processing.integration;

import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimResult;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobClaimService;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobCompletionService;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobFailureResult;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobFailureService;
import com.lorenzosanz.uploadservice.processing.application.ProcessingJobHeartbeatService;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJob;
import com.lorenzosanz.uploadservice.processing.domain.ProcessingJobStatus;
import com.lorenzosanz.uploadservice.processing.infrastructure.persistence.ProcessingJobRepository;
import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;
import com.lorenzosanz.uploadservice.upload.domain.Upload;
import com.lorenzosanz.uploadservice.upload.infrastructure.persistence.UploadRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class ProcessingJobClaimConcurrencyIntegrationTest {

    @Autowired
    private ProcessingJobClaimService claimService;

    @Autowired
    private ProcessingJobHeartbeatService heartbeatService;

    @Autowired
    private ProcessingJobCompletionService completionService;

    @Autowired
    private ProcessingJobFailureService failureService;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    void shouldAllowOnlyOneWorkerToClaimSameJobConcurrently()
            throws Exception {

        UUID uploadId = createJob();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<ProcessingJobClaimResult> claimTask = () -> {
                ready.countDown();

                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Workers did not start simultaneously"
                    );
                }

                return claimService.claim(
                        uploadId,
                        Duration.ofMinutes(2)
                );
            };

            Future<ProcessingJobClaimResult> firstWorker =
                    executor.submit(claimTask);

            Future<ProcessingJobClaimResult> secondWorker =
                    executor.submit(claimTask);

            assertTrue(ready.await(5, TimeUnit.SECONDS));

            start.countDown();

            ProcessingJobClaimResult firstResult =
                    firstWorker.get(15, TimeUnit.SECONDS);

            ProcessingJobClaimResult secondResult =
                    secondWorker.get(15, TimeUnit.SECONDS);

            boolean firstClaimed =
                    firstResult instanceof ProcessingJobClaimResult.Claimed;

            boolean secondClaimed =
                    secondResult instanceof ProcessingJobClaimResult.Claimed;

            assertTrue(
                    firstClaimed != secondClaimed,
                    "Exactly one worker must claim the job"
            );

            ProcessingJobClaimResult winner =
                    firstClaimed ? firstResult : secondResult;

            ProcessingJobClaimResult loser =
                    firstClaimed ? secondResult : firstResult;

            ProcessingJob winningJob = assertInstanceOf(
                    ProcessingJobClaimResult.Claimed.class,
                    winner
            ).job();

            assertInstanceOf(
                    ProcessingJobClaimResult.LeaseActive.class,
                    loser
            );

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    persisted.getStatus()
            );

            assertEquals(1, persisted.getAttempts());
            assertNotNull(persisted.getClaimToken());

            assertEquals(
                    winningJob.getClaimToken(),
                    persisted.getClaimToken()
            );

        } finally {
            start.countDown();

            executor.shutdown();

            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            }

            cleanUp(uploadId);
        }
    }

    @Test
    void shouldRecoverExpiredLeaseAndRejectPreviousWorker()
            throws InterruptedException {

        UUID uploadId = createJob();

        try {
            ProcessingJob firstClaim = assertInstanceOf(
                    ProcessingJobClaimResult.Claimed.class,
                    claimService.claim(
                            uploadId,
                            Duration.ofMillis(250)
                    )
            ).job();

            UUID oldToken = firstClaim.getClaimToken();
            UUID jobId = firstClaim.getJobId();

            assertEquals(1, firstClaim.getAttempts());

            Thread.sleep(500);

            ProcessingJob secondClaim = assertInstanceOf(
                    ProcessingJobClaimResult.Claimed.class,
                    claimService.claim(
                            uploadId,
                            Duration.ofMinutes(2)
                    )
            ).job();

            UUID newToken = secondClaim.getClaimToken();

            assertTrue(!oldToken.equals(newToken));
            assertEquals(2, secondClaim.getAttempts());

            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    secondClaim.getStatus()
            );

            assertFalse(heartbeatService.heartbeat(
                    jobId,
                    oldToken,
                    Duration.ofMinutes(2)
            ));

            assertFalse(completionService.complete(
                    jobId,
                    oldToken,
                    "processed/" + uploadId + ".mp4"
            ));

            ProcessingJobFailureResult failureResult =
                    failureService.fail(
                            jobId,
                            oldToken,
                            "Late failure from previous worker",
                            true
                    );

            assertInstanceOf(
                    ProcessingJobFailureResult.Rejected.class,
                    failureResult
            );

            ProcessingJob persisted = processingJobRepository
                    .findByUploadId(uploadId)
                    .orElseThrow();

            assertEquals(
                    ProcessingJobStatus.PROCESSING,
                    persisted.getStatus()
            );

            assertEquals(2, persisted.getAttempts());
            assertEquals(newToken, persisted.getClaimToken());

            assertTrue(heartbeatService.heartbeat(
                    jobId,
                    newToken,
                    Duration.ofMinutes(2)
            ));

        } finally {
            cleanUp(uploadId);
        }
    }

    private UUID createJob() {
        UUID uploadId = UUID.randomUUID();

        Upload upload = new Upload(
                uploadId,
                "concurrency-test-" + uploadId + ".mp4",
                "video/mp4",
                1024L,
                Instant.now()
        );

        uploadRepository.saveAndFlush(upload);

        ProcessingJob job = new ProcessingJob(
                UUID.randomUUID(),
                uploadId,
                Instant.now()
        );

        processingJobRepository.saveAndFlush(job);

        return uploadId;
    }

    private void cleanUp(UUID uploadId) {
        processingJobRepository.findByUploadId(uploadId)
                .ifPresent(job ->
                        processingJobRepository.deleteById(job.getJobId())
                );

        if (uploadRepository.existsById(uploadId)) {
            uploadRepository.deleteById(uploadId);
        }
    }
}
