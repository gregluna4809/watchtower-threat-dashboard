CREATE TABLE remote_endpoints (
    id BIGSERIAL PRIMARY KEY,
    ip INET UNIQUE NOT NULL,
    asn INTEGER NULL,
    asn_org TEXT NULL,
    country_iso CHAR(2) NULL,
    country_name TEXT NULL,
    city TEXT NULL,
    reverse_dns TEXT NULL,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL
);

