CREATE TABLE processes (
    id BIGSERIAL PRIMARY KEY,
    pid INTEGER NOT NULL,
    name TEXT NOT NULL,
    path TEXT NULL,
    signed BOOLEAN NULL,
    signer TEXT NULL,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_processes_pid_name_first_seen
    ON processes (pid, name, first_seen);

