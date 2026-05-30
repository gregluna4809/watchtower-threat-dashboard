CREATE TABLE honeypot_hits (
    id BIGSERIAL PRIMARY KEY,
    source_ip INET NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    request_path TEXT NOT NULL,
    user_agent TEXT NULL,
    method VARCHAR(16) NOT NULL
);

CREATE INDEX idx_honeypot_hits_observed_at
    ON honeypot_hits (observed_at DESC);

CREATE INDEX idx_honeypot_hits_source_ip
    ON honeypot_hits (source_ip);
