package com.lorenzosanz.uploadservice.validation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.lorenzosanz.uploadservice.exception.FileValidationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void shouldAcceptPngWithCorrectSignature() {
        byte[] content = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                content
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldAcceptJpegWithCorrectSignature() {
        byte[] content = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                content
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldAcceptGif87aWithCorrectSignature() {
        byte[] content = {
                'G', 'I', 'F', '8', '7', 'a'
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.gif",
                "image/gif",
                content
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldAcceptGif89aWithCorrectSignature() {
        byte[] content = {
                'G', 'I', 'F', '8', '9', 'a'
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.gif",
                "image/gif",
                content
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldAcceptWebpWithRiffAndWebpIdentifiers() {
        byte[] content = {
                'R', 'I', 'F', 'F',
                0, 0, 0, 0,
                'W', 'E', 'B', 'P',
                'V', 'P', '8', ' '
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.webp",
                "image/webp",
                content
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldRejectWebpWhenRiffIdentifierIsMissing() {
        byte[] content = {
                'N', 'O', 'P', 'E',
                0, 0, 0, 0,
                'W', 'E', 'B', 'P'
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.webp",
                "image/webp",
                content
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldRejectWebpWhenWebpIdentifierIsMissing() {
        byte[] content = {
                'R', 'I', 'F', 'F',
                0, 0, 0, 0,
                'N', 'O', 'P', 'E'
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.webp",
                "image/webp",
                content
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldRejectWebpWithTruncatedHeader() {
        byte[] content = {
                'R', 'I', 'F', 'F',
                0, 0, 0, 0
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "truncated.webp",
                "image/webp",
                content
        );

        assertThrows(
                FileValidationException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void shouldRejectNullContentType() {
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
    void shouldRejectUnsupportedContentType() {
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
    void shouldRejectNullFile() {
        assertThrows(
                FileValidationException.class,
                () -> validator.validate(null)
        );
    }
}