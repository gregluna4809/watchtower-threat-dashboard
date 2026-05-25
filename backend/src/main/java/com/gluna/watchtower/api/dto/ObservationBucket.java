package com.gluna.watchtower.api.dto;

import java.time.OffsetDateTime;

public record ObservationBucket(
        OffsetDateTime bucketStart,
        OffsetDateTime bucketEnd,
        long count
) {
}
