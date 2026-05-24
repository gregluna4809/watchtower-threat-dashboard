package com.gluna.watchtower.api.dto;

import java.time.OffsetDateTime;

public record ConnectionDto(
        Long id,
        String protocol,
        String state,
        String localIp,
        Integer localPort,
        String remoteIp,
        Integer remotePort,
        ProcessSummary process,
        EndpointSummary endpoint,
        Integer latestScore,
        OffsetDateTime firstSeen,
        OffsetDateTime lastSeen,
        Integer observationCount
) {
}

