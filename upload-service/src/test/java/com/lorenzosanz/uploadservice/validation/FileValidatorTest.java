package com.lorenzosanz.uploadservice.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.lorenzosanz.uploadservice.config.StorageProperties;

class FileValidatorTest {

    @Test
    void shouldAcceptMp4WithinConfiguredSizeLimit() {
        StorageProperties properties = new StorageProperties();
        properties.setMaxFileSize(1024);

        FileValidator validator = new FileValidator(properties);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.mp4",
                "video/mp4",
                new byte[] { 1, 2, 3, 4 }
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }
}