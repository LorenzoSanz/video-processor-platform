package com.lorenzosanz.uploadservice.validation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.lorenzosanz.uploadservice.config.StorageProperties;
import com.lorenzosanz.uploadservice.exception.FileValidationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileValidatorTest {

    private FileValidator createValidator(long maxFileSize) {
        StorageProperties properties = new StorageProperties();
        properties.setMaxFileSize(maxFileSize);

        return new FileValidator(properties);
    }

    @Test
    void shouldAcceptMp4WithinConfiguredSizeLimit() {
        FileValidator validator = createValidator(1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.mp4",
                "video/mp4",
                new byte[] { 1, 2, 3, 4 }
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldRejectNullFile() {
        FileValidator validator = createValidator(1024);

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(null)
        );
    }

    @Test
    void shouldRejectEmptyFile() {
        FileValidator validator = createValidator(1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldRejectFileExceedingConfiguredSizeLimit() {
        FileValidator validator = createValidator(3);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.mp4",
                "video/mp4",
                new byte[] { 1, 2, 3, 4 }
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldRejectNullContentType() {
        FileValidator validator = createValidator(1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                null,
                new byte[] { 1, 2, 3, 4 }
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldRejectBlankContentType() {
        FileValidator validator = createValidator(1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "   ",
                new byte[] { 1, 2, 3, 4 }
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        FileValidator validator = createValidator(1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                new byte[] { 1, 2, 3, 4 }
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldAcceptAllowedContentTypeRegardlessOfCase() {
        FileValidator validator = createValidator(1024);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "IMAGE/PNG",
                new byte[] { 1, 2, 3, 4 }
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }
}