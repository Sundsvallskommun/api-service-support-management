ALTER TABLE
    errand_labels ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

CREATE
    TABLE
        errand_label_permissions(
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            errand_label_id BIGINT NOT NULL,
            permission VARCHAR(255) NOT NULL,
            CONSTRAINT fk_errand_label FOREIGN KEY(errand_label_id) REFERENCES errand_labels(id) ON
            DELETE
                CASCADE
        );