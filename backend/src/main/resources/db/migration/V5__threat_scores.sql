CREATE TABLE threat_scores (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    score INTEGER NOT NULL,
    reasons JSONB NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_threat_scores_target_computed_at
    ON threat_scores (target_type, target_id, computed_at DESC);

