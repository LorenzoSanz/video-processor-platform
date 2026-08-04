package com.lorenzosanz.uploadservice.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lorenzosanz.uploadservice.exception.FileValidationException;

@Service
public final class FileSignatureValidator {

    private static final Map<String, List<byte[]>> ALLOWED_SIGNATURES = Map.of(

            "image/png", List.of(
                    new byte[] {
                            (byte) 0x89, 0x50, 0x4E, 0x47,
                            0x0D, 0x0A, 0x1A, 0x0A
                    }),

            "image/jpeg", List.of(
                    new byte[] {
                            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
                    }),

            "image/webp", List.of(
                    new byte[] {
                            0x52, 0x49, 0x46, 0x46
                    }),

            "image/gif", List.of(
                    new byte[] {
                            0x47, 0x49, 0x46, 0x38, 0x37, 0x61
                    },
                    new byte[] {
                            0x47, 0x49, 0x46, 0x38, 0x39, 0x61
                    })
    );

    public void validate(MultipartFile file) {

        List<byte[]> expectedSignatures =
                ALLOWED_SIGNATURES.get(file.getContentType());

        if (expectedSignatures == null) {
            throw new FileValidationException("Unsupported content type.");
        }

        try (InputStream inputStream = file.getInputStream()) {

            byte[] fileHeader = inputStream.readNBytes(8);

            for (byte[] signature : expectedSignatures) {

                byte[] fileSignature =
                        Arrays.copyOf(fileHeader, signature.length);

                if (Arrays.equals(fileSignature, signature)) {
                    return;
                }
            }

        } catch (IOException e) {
            throw new FileValidationException(
                    "Unable to read uploaded file.", e);
        }

        throw new FileValidationException(
                "File signature does not match its content type.");
    }
}
