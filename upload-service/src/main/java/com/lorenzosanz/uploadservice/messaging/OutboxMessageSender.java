package com.lorenzosanz.uploadservice.messaging;

public interface OutboxMessageSender {

    void send(OutboxMessage message);
}