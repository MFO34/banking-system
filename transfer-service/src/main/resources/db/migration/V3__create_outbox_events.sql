CREATE TABLE outbox_events (
    id            UUID        NOT NULL,
    aggregate_id  UUID        NOT NULL,
    event_type    VARCHAR(40) NOT NULL,
    payload       TEXT        NOT NULL,
    published     BOOLEAN     NOT NULL DEFAULT false,
    created_at    TIMESTAMP   NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_published ON outbox_events (published) WHERE published = false;
