
CREATE TABLE upload (
    upload_id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
