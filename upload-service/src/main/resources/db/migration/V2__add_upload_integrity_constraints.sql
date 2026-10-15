ALTER TABLE upload
    ADD CONSTRAINT chk_upload_size_positive
    CHECK (size > 0);

ALTER TABLE upload
    ADD CONSTRAINT chk_upload_file_name_not_blank
    CHECK (btrim(file_name) <> '');