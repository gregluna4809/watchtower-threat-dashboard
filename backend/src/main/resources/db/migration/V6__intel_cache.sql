CREATE TABLE intel_cache (
    id BIGSERIAL PRIMARY KEY,
    ip INET NOT NULL,
    source VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL,
    ttl_seconds INTEGER NOT NULL,
    CONSTRAINT uq_intel_cache_ip_source UNIQUE (ip, source)
);

