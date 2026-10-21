package com.lorenzosanz.uploadservice.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "scheduling-enabled",
        havingValue = "true"
)
public class OutboxPublisherScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxPublisherService publisherService;
    private final int batchSize;
    private final long leaseDurationSeconds;

    public OutboxPublisherScheduler(
            OutboxPublisherService publisherService,
            @Value("${outbox.publisher.batch-size:10}") int batchSize,
            @Value("${outbox.publisher.lease-duration-seconds:30}")
                    long leaseDurationSeconds) {

        this.publisherService = publisherService;
        this.batchSize = batchSize;
        this.leaseDurationSeconds = leaseDurationSeconds;
    }

    @Scheduled(
            fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}"
    )
    public void publishPendingEvents() {
        try {
            int published = publisherService.publishPending(
                    batchSize,
                    Duration.ofSeconds(leaseDurationSeconds)
            );

            if (published > 0) {
                logger.info(
                        "Published {} outbox events",
                        published
                );
            }
        } catch (RuntimeException exception) {
            logger.error(
                    "Outbox publishing cycle failed",
                    exception
            );
        }
    }
}