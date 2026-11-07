
package com.lorenzosanz.uploadservice.processing.application.port;

import java.util.Objects;

public sealed interface ProcessingResult
        permits ProcessingResult.Succeeded,
                ProcessingResult.Failed {

    record Succeeded(String outputPath)
            implements ProcessingResult {

        public Succeeded {
            if (outputPath == null || outputPath.isBlank()) {
                throw new IllegalArgumentException(
                        "Output path is required"
                );
            }
        }
    }

    record Failed(
            String errorMessage,
            FailureKind failureKind
    ) implements ProcessingResult {

        public Failed {
            Objects.requireNonNull(
                    failureKind,
                    "Failure kind is required"
            );

            if (errorMessage == null || errorMessage.isBlank()) {
                throw new IllegalArgumentException(
                        "Error message is required"
                );
            }
        }
    }

    enum FailureKind {
        RETRYABLE,
        PERMANENT
    }
}
