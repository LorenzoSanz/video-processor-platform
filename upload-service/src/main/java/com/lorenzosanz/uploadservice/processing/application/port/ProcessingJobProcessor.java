
package com.lorenzosanz.uploadservice.processing.application.port;

public interface ProcessingJobProcessor {

    ProcessingResult process(
            ProcessingCommand command,
            ProcessingCancellationToken cancellationToken
    );
}
