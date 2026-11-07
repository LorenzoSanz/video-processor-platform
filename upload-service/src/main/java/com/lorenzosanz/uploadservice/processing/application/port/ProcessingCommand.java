
package com.lorenzosanz.uploadservice.processing.application.port;

import java.util.Objects;
import java.util.UUID;

public record ProcessingCommand(
        UUID jobId,
        UUID uploadId,
        UUID claimToken
) {

    public ProcessingCommand {
        Objects.requireNonNull(jobId, "Job ID is required");
        Objects.requireNonNull(uploadId, "Upload ID is required");
        Objects.requireNonNull(claimToken, "Claim token is required");
    }
}
