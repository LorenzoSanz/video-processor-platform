package com.lorenzosanz.uploadservice.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lorenzosanz.uploadservice.enums.UploadStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UploadValidationTest {
      private Validator validator;

    @BeforeEach
    void setUp() {
      ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
      
      validator = validatorFactory.getValidator();
    }

    @Test
    void shouldAcceptValidUpload() {
        Upload upload = new Upload(
            UUID.randomUUID(),
            "test.mp4",
            UploadStatus.PENDING,
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
        Upload upload = new Upload(
            UUID.randomUUID(),
            "test.mp4",
            UploadStatus.PENDING,
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

    Upload upload = new Upload(
        UUID.randomUUID(),
        "test.mp4",
        UploadStatus.PENDING,
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
        Upload upload = new Upload(
            UUID.randomUUID(),
            "",
            UploadStatus.PENDING,
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

        Upload upload = new Upload(
            UUID.randomUUID(),
            longFileName,
            UploadStatus.PENDING,
            "video/mp4",
            1000L,
            Instant.now()
        );

        Set<ConstraintViolation<Upload>> violations =
            validator.validate(upload);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectNullStatus() {
        Upload upload = new Upload(
            UUID.randomUUID(),
            "test.mp4",
            null,
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
        Upload upload = new Upload(
            UUID.randomUUID(),
            "test.mp4",
            UploadStatus.PENDING,
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
        Upload upload = new Upload(
            UUID.randomUUID(),
            "test.mp4",
            UploadStatus.PENDING,
            "video/mp4",
            1000L,
            null
        );

        Set<ConstraintViolation<Upload>> violations =
            validator.validate(upload);

        assertFalse(violations.isEmpty());
    }
}
