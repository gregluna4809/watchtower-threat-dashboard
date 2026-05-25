package com.gluna.watchtower.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ThreatScoreDto(
        Integer score,
        List<Map<String, Object>> reasons,
        OffsetDateTime computedAt
) {
}
