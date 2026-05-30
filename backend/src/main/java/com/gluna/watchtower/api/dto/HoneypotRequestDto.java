package com.gluna.watchtower.api.dto;

import java.time.OffsetDateTime;

public record HoneypotRequestDto(
        Long id,
        String sourceIp,
        OffsetDateTime observedAt,
        String requestPath,
        String userAgent,
        String method
) {
}
