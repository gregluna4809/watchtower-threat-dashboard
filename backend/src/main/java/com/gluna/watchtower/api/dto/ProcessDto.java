package com.gluna.watchtower.api.dto;

import java.time.OffsetDateTime;

public record ProcessDto(
        Long id,
        Integer pid,
        String name,
        String path,
        Boolean signed,
        String signer,
        Long connectionCount,
        Integer maxScore,
        OffsetDateTime firstSeen,
        OffsetDateTime lastSeen
) {
}

