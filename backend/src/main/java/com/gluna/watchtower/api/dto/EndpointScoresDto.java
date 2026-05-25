package com.gluna.watchtower.api.dto;

import java.util.List;

public record EndpointScoresDto(
        ThreatScoreDto latest,
        List<ThreatScoreDto> history
) {
}
