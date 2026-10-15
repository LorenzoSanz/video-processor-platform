package com.lorenzosanz.uploadservice.repository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class UploadDatabaseConstraintsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertUpload(String fileName, long size) {
        String sql = """
                INSERT INTO upload (
                    upload_id,
                    file_name,
                    storage_path,
                    status,
                    content_type,
                    size,
                    created_at
                )
                VALUES (?, ?, NULL, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        jdbcTemplate.update(
                sql,
                UUID.randomUUID(),
                fileName,
                "PENDING",
                "video/mp4",
                size
        );
    }

    @Test
    void shouldRejectZeroSizeAtDatabaseLevel() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertUpload("video.mp4", 0)
        );
    }

    @Test
    void shouldRejectNegativeSizeAtDatabaseLevel() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertUpload("video.mp4", -5)
        );
    }

    @Test
    void shouldRejectEmptyFileNameAtDatabaseLevel() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertUpload("", 100)
        );
    }

    @Test
    void shouldRejectBlankFileNameAtDatabaseLevel() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertUpload("   ", 100)
        );
    }
}