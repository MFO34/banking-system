CREATE TABLE idempotency_records (
    idempotency_key  UUID      NOT NULL,
    response         TEXT      NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    PRIMARY KEY (idempotency_key)
);
