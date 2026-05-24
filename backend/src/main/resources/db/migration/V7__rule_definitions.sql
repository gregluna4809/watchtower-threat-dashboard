CREATE TABLE rule_definitions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    description TEXT NOT NULL,
    default_points INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO rule_definitions (code, display_name, description, default_points, enabled)
VALUES
    (
        'UNSIGNED_OUTBOUND',
        'Unsigned outbound process',
        'Adds risk when an unsigned process has an established outbound connection.',
        20,
        true
    ),
    (
        'TEMP_PATH_EXEC',
        'Temporary path executable',
        'Adds risk when a process executable is running from a temporary or user application data path.',
        30,
        true
    ),
    (
        'UNCOMMON_PORT',
        'Uncommon remote port',
        'Adds risk when a connection uses a remote port outside the common allowlist.',
        5,
        true
    ),
    (
        'HIGH_RISK_COUNTRY',
        'High-risk country endpoint',
        'Adds risk when a remote endpoint geolocates to a configured high-risk country.',
        15,
        true
    ),
    (
        'ABUSEIPDB_FLAG',
        'AbuseIPDB flagged endpoint',
        'Adds scaled risk when AbuseIPDB reports meaningful abuse confidence for a remote endpoint.',
        40,
        true
    ),
    (
        'PRIVATE_RANGE_BROWSER',
        'Browser private-range connection',
        'Adds risk when a known browser connects to a private network range outside the local subnet.',
        10,
        true
    ),
    (
        'BEACON_CADENCE',
        'Beacon-like cadence',
        'Adds risk when the same process and endpoint communicate at a regular repeated interval.',
        20,
        false
    );

