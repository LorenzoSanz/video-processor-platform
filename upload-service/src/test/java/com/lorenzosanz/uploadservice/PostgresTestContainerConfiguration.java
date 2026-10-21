package com.lorenzosanz.uploadservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") 
    // Spring Boot handles life cycle of the bean itself.
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18")
                .withDatabaseName("upload_platform_test")
                .withUsername("test")
                .withPassword("test");
    }
}
