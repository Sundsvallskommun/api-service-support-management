CREATE TABLE handover_idempotency (
    id                   VARCHAR(36)  NOT NULL,
    idempotency_key      VARCHAR(255) NOT NULL,
    new_errand_id        VARCHAR(36),
    new_errand_number    VARCHAR(255),
    target_namespace     VARCHAR(255),
    target_municipality_id VARCHAR(16),
    relation_id          VARCHAR(255),
    warnings             LONGTEXT,
    created_at           DATETIME(6),
    expires_at           DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_handover_idempotency_key (idempotency_key),
    INDEX idx_handover_idempotency_expires_at (expires_at)
);
