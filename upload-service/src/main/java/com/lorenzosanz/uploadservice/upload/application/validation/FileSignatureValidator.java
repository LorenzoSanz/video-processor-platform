package com.lorenzosanz.uploadservice.upload.application.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.upload.exception.FileValidationException;

/**
 * Performs a preliminary file-signature check only.
 * Not guarantees that the file is structurally valid,
 * intact, or safe to process.
 */
@Service
public final class FileSignatureValidator {

    private static final String MP4_CONTENT_TYPE = "video/mp4";
    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private static final byte[] MP4_FTYP_SIGNATURE = {
            'f', 't', 'y', 'p'
    };

    private static final byte[] WEBP_RIFF_SIGNATURE = {
            'R', 'I', 'F', 'F'
    };

    private static final byte[] WEBP_FORMAT_SIGNATURE = {
            'W', 'E', 'B', 'P'
    };

    private static final Map<String, List<byte[]>> ALLOWED_SIGNATURES =
            Map.of(
                    "image/png", List.of(
                            new byte[] {
                                    (byte) 0x89, 0x50, 0x4E, 0x47,
                                    0x0D, 0x0A, 0x1A, 0x0A
                            }
                    ),
                    "image/jpeg", List.of(
                            new byte[] {
                                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
                            }
                    ),
                    "image/gif", List.of(
                            new byte[] {
                                    'G', 'I', 'F', '8', '7', 'a'
                            },
                            new byte[] {
                                    'G', 'I', 'F', '8', '9', 'a'
                            }
                    )
            );

    public void validate(MultipartFile file) {
        if (file == null) {
            throw new FileValidationException("File is required.");
        }

        String declaredContentType = file.getContentType();

        if (declaredContentType == null || declaredContentType.isBlank()) {
            throw new FileValidationException("Unsupported content type.");
        }

        String contentType = declaredContentType
                .trim()
                .toLowerCase(Locale.ROOT);

        boolean isMp4 = MP4_CONTENT_TYPE.equals(contentType);
        boolean isWebp = WEBP_CONTENT_TYPE.equals(contentType);

        List<byte[]> expectedSignatures = ALLOWED_SIGNATURES.get(contentType);

        if (expectedSignatures == null && !isMp4 && !isWebp) {
            throw new FileValidationException("Unsupported content type.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Necessary for WebP format, it needs at least 12 bytes.
            byte[] fileHeader = inputStream.readNBytes(isWebp ? 12 : 8);

            if (isMp4) {
                if (fileHeader.length >= 8
                        && Arrays.equals(
                                Arrays.copyOfRange(fileHeader, 4, 8),
                                MP4_FTYP_SIGNATURE)) {
                    return;
                }
            } else if (isWebp) {
                if (hasWebpSignature(fileHeader)) {
                    return;
                }
            } else {
                for (byte[] signature : expectedSignatures) {
                    if (fileHeader.length >= signature.length
                            && Arrays.equals(
                                    Arrays.copyOfRange(
                                            fileHeader,
                                            0,
                                            signature.length
                                    ),
                                    signature)) {
                        return;
                    }
                }
            }

        } catch (IOException exception) {
            throw new FileValidationException(
                    "Unable to read uploaded file.",
                    exception
            );
        }

        throw new FileValidationException(
                "File signature does not match its content type."
        );
    }

    private static boolean hasWebpSignature(byte[] fileHeader) {
        return fileHeader.length >= 12
                && Arrays.equals(
                        Arrays.copyOfRange(fileHeader, 0, 4),
                        WEBP_RIFF_SIGNATURE
                )
                && Arrays.equals(
                        Arrays.copyOfRange(fileHeader, 8, 12),
                        WEBP_FORMAT_SIGNATURE
                );
    }
}