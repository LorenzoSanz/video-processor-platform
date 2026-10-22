package com.lorenzosanz.uploadservice.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.lorenzosanz.uploadservice.exception.FileValidationException;

class FileSignatureValidatorTest {

    private final FileSignatureValidator validator =
            new FileSignatureValidator();

    @Test
    void shouldAcceptMp4WithFtypBox() {
        byte[] header = {
                0, 0, 0, 24,
                'f', 't', 'y', 'p',
                'i', 's', 'o', 'm',
                0, 0, 0, 0
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.mp4",
                "video/mp4",
                header
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldRejectMp4WithoutFtypBox() {
        byte[] content = {
                0, 0, 0, 24,
                'n', 'o', 'p', 'e',
                1, 2, 3, 4
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.mp4",
                "video/mp4",
                content
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }
}