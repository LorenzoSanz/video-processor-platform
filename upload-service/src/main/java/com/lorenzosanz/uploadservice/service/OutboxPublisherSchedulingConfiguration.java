package com.lorenzosanz.uploadservice.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "scheduling-enabled",
        havingValue = "true"
)
public class OutboxPublisherSchedulingConfiguration {
}