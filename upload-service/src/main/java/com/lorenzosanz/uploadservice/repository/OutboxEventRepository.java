package com.lorenzosanz.uploadservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lorenzosanz.uploadservice.entity.OutboxEvent;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {
}