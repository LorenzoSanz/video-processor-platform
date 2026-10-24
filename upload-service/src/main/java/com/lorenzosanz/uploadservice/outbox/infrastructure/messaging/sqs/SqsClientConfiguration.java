package com.lorenzosanz.uploadservice.outbox.infrastructure.messaging.sqs;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "messaging.sqs",
        name = "enabled",
        havingValue = "true"
)
public class SqsClientConfiguration {

    @Bean
    SqsClient sqsClient(
            @Value("${messaging.sqs.endpoint:}") String endpoint,
            @Value("${messaging.sqs.region:eu-west-1}") String region,
            @Value("${messaging.sqs.access-key:}") String accessKey,
            @Value("${messaging.sqs.secret-key:}") String secretKey) {

        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region));

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (accessKey.isBlank() != secretKey.isBlank()) {
            throw new IllegalStateException(
                    "SQS access key and secret key must be configured together"
            );
        }

        if (!accessKey.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    accessKey,
                                    secretKey
                            )
                    )
            );
        }

        return builder.build();
    }
}