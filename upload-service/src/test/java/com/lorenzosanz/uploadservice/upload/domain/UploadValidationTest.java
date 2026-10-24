package com.lorenzosanz.uploadservice.upload.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UploadValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory =
                Validation.buildDefaultValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @Test
    void shouldAcceptValidUpload() {
        Upload upload = createUpload(
                "test.mp4",
                "video/mp4",
                1000L,
                Instant.now()
        );

        Set<ConstraintViolation<Upload>> violations =
                validator.validate(upload);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectNegativeSize() {
        Upload upload = createUpload(
                "test.mp4",
                "video/mp4",
                -100L,
                Instant.now()
        );

        Set<ConstraintViolation<Upload>> violations =
                validator.validate(upload);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectInvalidSize() {
        Upload upload = createUpload(
                "test.mp4",
                "video/mp4",
                0L,
                Instant.now()
        );

        Set<ConstraintViolation<Upload>> violations =
                validator.validate(upload);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectBlankFileName() {
        Upload upload = createUpload(
                "",
                "video/mp4",
                1000L,
                Instant.now()
        );

        Set<ConstraintViolation<Upload>> violations =
                validator.validate(upload);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectOversizedFileName() {
        String longFileName = "a".repeat(256);

        Upload upload = createUpload(
                longFileName,
                "video/mp4",
                1000L,
                Instant.now()
        );

        Set<ConstraintViolation<Upload>> violations =
                validator.validate(upload);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectBlankContentType() {
        Upload upload = createUpload(
                "test.mp4",
                "",
                1000L,
                Instant.now()
        );

        Set<ConstraintViolation<Upload>> violations =
                validator.validate(upload);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectNullCreatedAt() {
        Upload upload = createUpload(
                "test.mp4",
                "video/mp4",
                1000L,
                null
        );

        Set<ConstraintViolation<Upload>> violations =
                validator.validate(upload);

        assertFalse(violations.isEmpty());
    }

    private Upload createUpload(
            String fileName,
            String contentType,
            long size,
            Instant createdAt
    ) {
        return new Upload(
                UUID.randomUUID(),
                fileName,
                contentType,
                size,
                createdAt
        );
    }
}