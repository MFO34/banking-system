CREATE TABLE accounts (
    id          UUID        NOT NULL,
    owner_id    UUID        NOT NULL,
    balance     NUMERIC(19, 4) NOT NULL,
    currency    VARCHAR(3)  NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_balance CHECK (balance >= 0)
);
