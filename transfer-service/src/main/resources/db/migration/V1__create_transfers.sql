CREATE TABLE transfers (
    id               UUID           NOT NULL,
    from_account_id  UUID           NOT NULL,
    to_account_id    UUID           NOT NULL,
    amount           NUMERIC(19, 4) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    status           VARCHAR(30)    NOT NULL,
    created_at       TIMESTAMP      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_transfer_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'COMPENSATION_FAILED')),
    CONSTRAINT chk_transfer_amount CHECK (amount > 0)
);
