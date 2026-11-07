
package com.lorenzosanz.uploadservice.processing.application.port;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public interface ProcessingMessageQueue {

    List<ReceivedMessage> receive(
            int maxMessages,
            Duration waitTime,
            Duration visibilityTimeout
    );

    void delete(ReceivedMessage message);

    void changeVisibility(
            ReceivedMessage message,
            Duration visibilityTimeout
    );

    record ReceivedMessage(
            String messageId,
            String body,
            String receiptHandle
    ) {

        public ReceivedMessage {
            Objects.requireNonNull(messageId, "Message ID is required");
            Objects.requireNonNull(body, "Message body is required");
            Objects.requireNonNull(
                    receiptHandle,
                    "Receipt handle is required"
            );

            if (messageId.isBlank()
                    || body.isBlank()
                    || receiptHandle.isBlank()) {
                throw new IllegalArgumentException(
                        "Message fields must not be blank"
                );
            }
        }
    }
}
