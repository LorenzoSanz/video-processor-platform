package com.lorenzosanz.uploadservice.outbox.application.port;
import com.lorenzosanz.uploadservice.outbox.application.OutboxMessage;

public interface OutboxMessageSender {

    void send(OutboxMessage message);
}