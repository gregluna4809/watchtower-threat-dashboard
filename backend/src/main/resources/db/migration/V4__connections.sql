CREATE TABLE connections (
    id BIGSERIAL PRIMARY KEY,
    process_id BIGINT NULL REFERENCES processes (id),
    endpoint_id BIGINT NULL REFERENCES remote_endpoints (id),
    local_ip INET NOT NULL,
    local_port INTEGER NOT NULL,
    remote_port INTEGER NULL,
    protocol VARCHAR(8) NOT NULL,
    state VARCHAR(16) NULL,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,
    observation_count INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uq_connections_dedupe UNIQUE (
        process_id,
        local_port,
        endpoint_id,
        remote_port,
        protocol
    )
);

