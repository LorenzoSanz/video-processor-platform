
package com.lorenzosanz.uploadservice.processing.application.port;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

public final class ProcessingCancellationToken {

    private final AtomicReference<CancellationReason> cancellationReason =
            new AtomicReference<>();

    public boolean cancel(CancellationReason reason) {
        if (reason == null) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required"
            );
        }

        return cancellationReason.compareAndSet(null, reason);
    }

    public boolean isCancellationRequested() {
        return cancellationReason.get() != null;
    }

    public Optional<CancellationReason> cancellationReason() {
        return Optional.ofNullable(cancellationReason.get());
    }

    public void throwIfCancellationRequested() {
        CancellationReason reason = cancellationReason.get();

        if (reason != null) {
            throw new CancellationException(
                    "Processing cancelled: " + reason
            );
        }
    }

    public enum CancellationReason {
        DATABASE_LEASE_LOST,
        MESSAGE_VISIBILITY_LOST,
        APPLICATION_SHUTDOWN
    }
}
